package ru.storage.filestorageservice.file.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.storage.filestorageservice.common.security.CurrentUserProvider;
import ru.storage.filestorageservice.common.security.exception.AuthenticationException;
import ru.storage.filestorageservice.file.entity.FileMetadataEntity;
import ru.storage.filestorageservice.file.repository.FileMetadataRepository;
import ru.storage.filestorageservice.file.service.FileService;
import ru.storage.filestorageservice.file.service.context.FileContext;
import ru.storage.filestorageservice.file.service.dto.FileUploadDto;
import ru.storage.filestorageservice.file.service.exception.FileAlreadyExistsException;
import ru.storage.filestorageservice.file.service.exception.FileDeleteException;
import ru.storage.filestorageservice.file.service.exception.FileNotFoundException;
import ru.storage.filestorageservice.file.service.exception.FileUploadException;
import ru.storage.filestorageservice.file.service.listeners.event.FileUploadRollbackEvent;
import ru.storage.filestorageservice.outbox.entity.DeleteEventEntity;
import ru.storage.filestorageservice.outbox.repository.DeleteEventRepository;
import ru.storage.filestorageservice.storage.client.FileObjectStorageClient;
import ru.storage.filestorageservice.storage.exception.ObjectStorageUploadException;
import ru.storage.filestorageservice.storage.validation.FileValidationPolicy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private static final int BUFFER_SIZE = 65536;
    private final FileObjectStorageClient minioClient;
    private final FileValidationPolicy validationPolicy;
    private final FileMetadataRepository fileMetadataRepository;
    private final CurrentUserProvider userProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final DeleteEventRepository deleteEventRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public FileUploadDto upload(MultipartFile file, String folderName) {

        FileContext context = FileContext.of(userProvider, file, folderName);
        checkFileNotExists(context);

        try (BufferedInputStream is = new BufferedInputStream(file.getInputStream(), BUFFER_SIZE)) {
            is.mark(BUFFER_SIZE);
            validationPolicy.validate(is, context.filename(), context.sizeBytes());
            is.reset();

            minioClient.put(context.storagePath(), is, context.sizeBytes(), context.contentType());
            eventPublisher.publishEvent(new FileUploadRollbackEvent(context.storagePath()));

            FileMetadataEntity entity = FileMetadataEntity.of(context);

            FileMetadataEntity saved = fileMetadataRepository.saveAndFlush(entity);
            entityManager.refresh(saved);

            log.info("File {} successfully uploaded", context.filename());
            return FileUploadDto.of(saved);

        } catch (ObjectStorageUploadException e) {
            log.error("Failed to store file in MinIO: {}", context.filename(), e);
            throw new FileUploadException("Failed to store file in MinIO: " + context.filename(), e);
        } catch (DataAccessException e) {
            log.error("Failed to save file metadata: {}", context.filename(), e);
            throw new FileUploadException("Failed to save file metadata: " + context.filename(), e);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", context.filename(), e);
            throw new FileUploadException("Failed to upload file: " + context.filename(), e);
        }
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        String userId = userProvider.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException("No authenticated user found"));

        FileMetadataEntity file = fileMetadataRepository.findByIdAndOwnerUserIdAndIsDeletedFalse(fileId, userId)
            .orElseThrow(() -> new FileNotFoundException("File not found or access denied"));

        file.markDeleted();
        try {
            fileMetadataRepository.save(file);
        } catch (OptimisticLockingFailureException e) {
            log.error("Optimistic lock exception while deleting file {}", fileId, e);
            throw new FileDeleteException("File was modified concurrently, please retry", e);
        }

        try {
            deleteEventRepository.save(DeleteEventEntity.pending(fileId, userId, file.getStoragePath()));
        } catch (DataAccessException e) {
            log.error("Failed to save deleteByEventId event for file {}", fileId, e);
            throw new FileUploadException("Failed to save deleteByEventId event", e);
        }
    }

    private void checkFileNotExists(FileContext context) {
        boolean exists = fileMetadataRepository.existsByOwnerUserIdAndFolderNameAndOriginalFileNameAndIsDeletedFalse(
            context.userId(),
            context.folderName(),
            context.filename()
        );
        if (exists) {
            log.warn("File with name: {} already exists (not deleted)", context.filename());
            throw new FileAlreadyExistsException("File with name: " + context.filename() + " already exists");
        }
    }
}
