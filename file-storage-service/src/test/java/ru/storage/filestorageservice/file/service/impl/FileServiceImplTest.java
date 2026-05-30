package ru.storage.filestorageservice.file.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.storage.filestorageservice.common.security.CurrentUserProvider;
import ru.storage.filestorageservice.common.security.exception.AuthenticationException;
import ru.storage.filestorageservice.file.entity.FileMetadataEntity;
import ru.storage.filestorageservice.file.repository.FileMetadataRepository;
import ru.storage.filestorageservice.file.service.dto.FileUploadDto;
import ru.storage.filestorageservice.file.service.exception.FileDeleteException;
import ru.storage.filestorageservice.file.service.exception.FileNotFoundException;
import ru.storage.filestorageservice.file.service.exception.FileUploadException;
import ru.storage.filestorageservice.outbox.entity.DeleteEventEntity;
import ru.storage.filestorageservice.outbox.repository.DeleteEventRepository;
import ru.storage.filestorageservice.storage.client.FileObjectStorageClient;
import ru.storage.filestorageservice.storage.exception.ObjectStorageUploadException;
import ru.storage.filestorageservice.storage.validation.FileValidationPolicy;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private final String userId = "user-123";
    private final String folderName = "tmp/process-456";
    private final String originalFileName = "document.pdf";
    private final String contentType = "application/pdf";
    private final long fileSize = 1024L;
    private final Instant fixedNow = Instant.now();
    private final UUID fileId = UUID.randomUUID();
    @Mock
    private EntityManager entityManager;
    @Mock
    private DeleteEventRepository deleteEventRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FileObjectStorageClient minioClient;
    @Mock
    private FileValidationPolicy validationPolicy;
    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private CurrentUserProvider userProvider;
    @InjectMocks
    private FileServiceImpl fileService;
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        multipartFile = new MockMultipartFile(
            "file",
            originalFileName,
            contentType,
            new byte[(int) fileSize]
        );
    }

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.saveAndFlush(any(FileMetadataEntity.class)))
            .willReturn(createSavedEntity());

        // when
        FileUploadDto result = fileService.upload(multipartFile, folderName);

        // then
        assertThat(result).isNotNull();
        assertThat(result.originalFileName()).isEqualTo(originalFileName);
        assertThat(result.contentType()).isEqualTo(contentType);
        assertThat(result.sizeBytes()).isEqualTo(fileSize);
        assertThat(result.createdAt()).isEqualTo(fixedNow.atOffset(java.time.ZoneOffset.UTC));

        verify(validationPolicy).validate(any(InputStream.class), eq(originalFileName), eq(fileSize));
        verify(minioClient).put(
            argThat(path -> path.startsWith(folderName + "/") && path.endsWith("/" + originalFileName)),
            any(InputStream.class), eq(fileSize), eq(contentType)
        );
        verify(fileMetadataRepository).saveAndFlush(any(FileMetadataEntity.class));
    }

    @Test
    @DisplayName("Should throw AuthenticationException when user not authenticated")
    void shouldThrowAuthenticationExceptionWhenUserNotFound() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> fileService.upload(multipartFile, folderName))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("No authenticated user found");

        verify(validationPolicy, never()).validate(any(), any(), anyLong());
        verify(minioClient, never()).put(any(), any(), anyLong(), any());
        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw FileValidationException when validation fails")
    void shouldThrowFileValidationExceptionWhenValidationFails() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        doThrow(new FileValidationException("Invalid content type"))
            .when(validationPolicy).validate(any(InputStream.class), eq(originalFileName), eq(fileSize));

        // when / then
        assertThatThrownBy(() -> fileService.upload(multipartFile, folderName))
            .isInstanceOf(FileValidationException.class)
            .hasMessage("Invalid content type");

        verify(minioClient, never()).put(any(), any(), anyLong(), any());
        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw FileUploadException when MinIO put fails")
    void shouldThrowFileUploadExceptionWhenMinioFails() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        doThrow(new ObjectStorageUploadException("MinIO error", new RuntimeException()))
            .when(minioClient).put(anyString(), any(InputStream.class), anyLong(), anyString());

        // when / then
        assertThatThrownBy(() -> fileService.upload(multipartFile, folderName))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Failed to store file in MinIO");

        verify(validationPolicy).validate(any(InputStream.class), eq(originalFileName), eq(fileSize));
        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw FileUploadException when database save fails")
    void shouldThrowFileUploadExceptionWhenDatabaseFails() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.saveAndFlush(any(FileMetadataEntity.class)))
            .willThrow(new DataAccessResourceFailureException("DB connection error"));

        // when / then
        assertThatThrownBy(() -> fileService.upload(multipartFile, folderName))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Failed to save file metadata");

        verify(minioClient).put(
            argThat(path -> path.startsWith(folderName + "/") && path.endsWith("/" + originalFileName)),
            any(InputStream.class), eq(fileSize), eq(contentType)
        );
    }

    @Test
    @DisplayName("Should throw FileUploadException when reading file fails")
    void shouldThrowFileUploadExceptionWhenReadFails() throws IOException {
        // given
        MultipartFile brokenFile = mock(MultipartFile.class);
        given(brokenFile.getOriginalFilename()).willReturn(originalFileName);
        given(brokenFile.getContentType()).willReturn(contentType);
        given(brokenFile.getSize()).willReturn(fileSize);
        given(brokenFile.getInputStream()).willThrow(new IOException("Stream error"));

        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));

        // when / then
        assertThatThrownBy(() -> fileService.upload(brokenFile, folderName))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Failed to upload file: " + originalFileName);

        verify(validationPolicy, never()).validate(any(), any(), anyLong());
        verify(minioClient, never()).put(any(), any(), anyLong(), any());
        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should pass correct storagePath to FileMetadataEntity")
    void shouldPassCorrectStoragePathToEntity() {
        // given
        ArgumentCaptor<FileMetadataEntity> entityCaptor = ArgumentCaptor.forClass(FileMetadataEntity.class);
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.saveAndFlush(any(FileMetadataEntity.class)))
            .willReturn(createSavedEntity());

        // when
        fileService.upload(multipartFile, folderName);

        // then
        verify(fileMetadataRepository).saveAndFlush(entityCaptor.capture());
        FileMetadataEntity savedEntity = entityCaptor.getValue();

        String expectedPathPrefix = folderName + "/";
        String expectedPathSuffix = "/" + originalFileName;
        assertThat(savedEntity.getStoragePath()).startsWith(expectedPathPrefix);
        assertThat(savedEntity.getStoragePath()).endsWith(expectedPathSuffix);
        assertThat(savedEntity.getOwnerUserId()).isEqualTo(userId);
        assertThat(savedEntity.getOriginalFileName()).isEqualTo(originalFileName);
        assertThat(savedEntity.getContentType()).isEqualTo(contentType);
        assertThat(savedEntity.getExtension()).isEqualTo("pdf");
        assertThat(savedEntity.getSizeBytes()).isEqualTo(fileSize);
    }

    @Test
    @DisplayName("Should mark file as deleted and create deleteByEventId event")
    void shouldMarkFileDeletedAndCreateEvent() {
        // given
        FileMetadataEntity entity = createSavedEntity(fileId);
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.findByIdAndOwnerUserIdAndIsDeletedFalse(fileId, userId))
            .willReturn(Optional.of(entity));

        // when
        fileService.delete(fileId);

        // then
        verify(fileMetadataRepository).save(entity);
        assertThat(entity.isDeleted()).isTrue();
        verify(deleteEventRepository).save(any(DeleteEventEntity.class));
    }

    @Test
    @DisplayName("Should throw FileDeleteException when optimistic lock fails")
    void shouldThrowFileDeleteExceptionOnOptimisticLock() {
        // given
        FileMetadataEntity entity = createSavedEntity(fileId);
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.findByIdAndOwnerUserIdAndIsDeletedFalse(fileId, userId))
            .willReturn(Optional.of(entity));
        doThrow(OptimisticLockingFailureException.class)
            .when(fileMetadataRepository).save(entity);

        // when / then
        assertThatThrownBy(() -> fileService.delete(fileId))
            .isInstanceOf(FileDeleteException.class)
            .hasMessageContaining("File was modified concurrently, please retry");

        verify(deleteEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw FileUploadException when event save fails")
    void shouldThrowFileUploadExceptionWhenEventSaveFails() {
        // given
        FileMetadataEntity entity = createSavedEntity(fileId);
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.findByIdAndOwnerUserIdAndIsDeletedFalse(fileId, userId))
            .willReturn(Optional.of(entity));
        doThrow(new DataAccessResourceFailureException("DB error"))
            .when(deleteEventRepository).save(any(DeleteEventEntity.class));

        // when / then
        assertThatThrownBy(() -> fileService.delete(fileId))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Failed to save deleteByEventId event");
    }

    @Test
    @DisplayName("Should throw AuthenticationException when user not authenticated during deleteByEventId")
    void shouldThrowAuthenticationExceptionWhenUserNotFoundDuringDelete() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> fileService.delete(fileId))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("No authenticated user found");

        verify(fileMetadataRepository, never()).findByIdAndOwnerUserIdAndIsDeletedFalse(any(), any());
        verify(fileMetadataRepository, never()).delete(any());
        verify(minioClient, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw FileNotFoundException when file does not exist or does not belong to user")
    void shouldThrowFileNotFoundExceptionWhenFileNotFound() {
        // given
        given(userProvider.getCurrentUserId()).willReturn(Optional.of(userId));
        given(fileMetadataRepository.findByIdAndOwnerUserIdAndIsDeletedFalse(fileId, userId))
            .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> fileService.delete(fileId))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessage("File not found or access denied");

        verify(fileMetadataRepository, never()).delete(any());
        verify(minioClient, never()).delete(any());
    }

    private FileMetadataEntity createSavedEntity() {
        FileMetadataEntity entity = new FileMetadataEntity(
            userId,
            originalFileName,
            folderName,
            contentType,
            "pdf",
            fileSize,
            folderName + "/" + originalFileName,
            null
        );
        try {
            var idField = FileMetadataEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, UUID.randomUUID());

            var createdAtField = FileMetadataEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, fixedNow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    private FileMetadataEntity createSavedEntity(UUID id) {
        FileMetadataEntity entity = new FileMetadataEntity(
            userId,
            originalFileName,
            folderName,
            contentType,
            "pdf",
            fileSize,
            folderName + "/" + originalFileName,
            null
        );
        try {
            var idField = FileMetadataEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);

            var createdAtField = FileMetadataEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, fixedNow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}
