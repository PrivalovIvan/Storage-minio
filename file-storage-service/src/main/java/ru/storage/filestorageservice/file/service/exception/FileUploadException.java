package ru.storage.filestorageservice.file.service.exception;

/**
 * Exception thrown when a file upload operation fails.
 * <p>
 * Possible causes:
 * <ul>
 *   <li>I/O error while reading the input stream</li>
 *   <li>Error while storing the file in object storage (MinIO)</li>
 *   <li>Error while persisting file metadata in the database</li>
 *   <li>Timeout or network issues</li>
 * </ul>
 * </p>
 */
public class FileUploadException extends FileException {
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
