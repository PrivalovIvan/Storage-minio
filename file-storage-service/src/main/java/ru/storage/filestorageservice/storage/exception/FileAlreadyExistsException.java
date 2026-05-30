package ru.storage.filestorageservice.storage.exception;

/**
 * Exception thrown when object already exists.
 */
public class FileAlreadyExistsException extends ObjectStorageException {

    public FileAlreadyExistsException(String storagePath, Throwable cause) {
        super("File already exists in storage: " + storagePath, cause);
    }
}
