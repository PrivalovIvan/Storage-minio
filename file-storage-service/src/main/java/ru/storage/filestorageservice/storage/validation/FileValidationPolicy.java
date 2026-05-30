package ru.storage.filestorageservice.storage.validation;

import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;

import java.io.InputStream;

/**
 * Defines the contract for validating a file before storage.
 * Implementations should verify size, extension, MIME type.
 */
public interface FileValidationPolicy {

    /**
     * Validates the file content and metadata.
     *
     * @param inputStream stream of the file content (must support mark/reset)
     * @param filename    original filename provided by the client
     * @param sizeBytes   actual file size in bytes
     * @throws FileValidationException if any validation rule is violated
     */
    void validate(InputStream inputStream, String filename, long sizeBytes);
}
