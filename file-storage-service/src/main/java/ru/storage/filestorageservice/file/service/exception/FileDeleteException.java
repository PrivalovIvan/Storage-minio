package ru.storage.filestorageservice.file.service.exception;

/**
 * Exception thrown when a file deletion operation fails.
 * <p>
 * Typically occurs when the physical file cannot be removed from object storage (MinIO)
 * after the file metadata has already been deleted from the database.
 * </p>
 */
public class FileDeleteException extends FileException {
    public FileDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
