package ru.storage.filestorageservice.file.service.exception;

/**
 * Base exception for all file-related operations (upload, deleteByEventId, retrieval, etc.).
 * <p>
 * This is an unchecked (runtime) exception. All specific exceptions in the file service
 * should extend this class.
 * </p>
 */
public class FileException extends RuntimeException {
    public FileException(String message) {
        super(message);
    }

    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
}
