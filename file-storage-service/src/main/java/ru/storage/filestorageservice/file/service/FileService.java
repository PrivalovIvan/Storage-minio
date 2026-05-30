package ru.storage.filestorageservice.file.service;

import org.springframework.web.multipart.MultipartFile;
import ru.storage.filestorageservice.common.security.exception.AuthenticationException;
import ru.storage.filestorageservice.file.service.dto.FileUploadDto;
import ru.storage.filestorageservice.file.service.exception.FileDeleteException;
import ru.storage.filestorageservice.file.service.exception.FileNotFoundException;
import ru.storage.filestorageservice.file.service.exception.FileUploadException;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;

import java.util.UUID;

/**
 * High-level service for file management operations.
 * <p>
 * This service orchestrates the complete file upload flow:
 * <ul>
 *   <li>Resolving the current user</li>
 *   <li>Validating the file (size, extension, MIME type)</li>
 *   <li>Storing the binary content in the object storage (MinIO)</li>
 *   <li>Persisting file metadata in the database</li>
 * </ul>
 * </p>
 */
public interface FileService {

    /**
     * Uploads a file into the system.
     * <p>
     * The file is first validated, then stored in the configured object storage
     * (MinIO) under a path derived from the {@code temporaryFolderId}. Metadata
     * about the file is saved in the database. The operation is transactional,
     * meaning database rollback will not automatically deleteByEventId the stored object
     * – that must be handled separately.
     * </p>
     *
     * @param file               the uploaded multipart file (cannot be {@code null})
     * @param folderName  logical folder identifier (e.g. temporary process folder),
     *                           used as a prefix in the storage path
     * @return metadata of the uploaded file (including the generated unique ID and timestamps)
     * @throws AuthenticationException if the current user is not authenticated
     * @throws FileValidationException if the file does not pass validation (size, extension, MIME type)
     * @throws FileUploadException if an I/O error occurs during reading or storage operations
     */
    FileUploadDto upload(MultipartFile file, String folderName);

    /**
     * Deletes a file by its unique identifier.
     * <p>
     * The deletion is performed only if the file belongs to the currently authenticated user.
     * The operation is transactional: the file metadata is marked as deleted (soft deleteByEventId)
     * in the database, and a deletion event is persisted in the outbox table. The actual
     * removal of the physical file from object storage (MinIO) is performed asynchronously
     * by a background worker.
     * </p>
     *
     * @param fileId the unique identifier of the file to deleteByEventId (must not be {@code null})
     * @throws AuthenticationException if the current user is not authenticated
     * @throws FileNotFoundException if no file with the given ID exists for the current user
     * @throws FileDeleteException if the optimistic lock fails or the deletion event cannot be saved
     */
    void delete(UUID fileId);
}
