package ru.storage.filestorageservice.storage.exception;

/**
 * Exception thrown when object upload to storage fails.
 *
 * <p>Typically occurs during streaming upload operations
 * caused by connectivity issues, invalid bucket configuration,
 * authentication failures, or storage provider errors.</p>
 */
public class ObjectStorageUploadException extends ObjectStorageException {

    public ObjectStorageUploadException(String objectKey, Throwable cause) {
        super("Failed to upload object: " + objectKey, cause);
    }
}
