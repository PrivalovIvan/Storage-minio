package ru.storage.filestorageservice.storage.exception;

/**
 * Base exception for all object storage related failures.
 *
 * <p>Used to encapsulate infrastructure-specific exceptions
 * thrown by underlying storage providers such as MinIO,
 * Amazon S3, FTP, or local file systems.</p>
 */
public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectStorageException(String message) {
        super(message);
    }
}
