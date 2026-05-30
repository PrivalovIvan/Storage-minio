package ru.storage.filestorageservice.storage.exception;

/**
 * Exception thrown when object deletion from storage fails.
 *
 * <p>Represents failures occurring during removal of
 * an object from underlying object storage.</p>
 */
public class ObjectStorageDeleteException extends ObjectStorageException {

    public ObjectStorageDeleteException(String objectKey, Throwable cause) {
        super("Failed to deleteByEventId object: " + objectKey, cause);
    }
}
