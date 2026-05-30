package ru.storage.filestorageservice.storage.exception;

/**
 * Exception is thrown when an object is moved incorrectly.
 */
public class ObjectStorageMoveException extends ObjectStorageException {
    public ObjectStorageMoveException(String objectKey, String source, String target, Throwable cause) {
        super("Failed to moved object: " + objectKey + ", source: " + source + ", target: " + target, cause);
    }
}
