package ru.storage.filestorageservice.storage.client;

import ru.storage.filestorageservice.storage.exception.ObjectStorageMoveException;

import java.io.InputStream;

/**
 * Abstraction over object storage operations.
 *
 * <p>Provides unified API for interacting with binary object storage
 * independently of underlying implementation details.</p>
 *
 * <p>Implementations may use MinIO, Amazon S3,
 * local file systems, FTP servers, or other storage providers.</p>
 */
public interface FileObjectStorageClient {
    /**
     * Uploads object into storage using streaming API.
     *
     * @param storagePath unique storage object path
     * @param inputStream object content stream
     * @param sizeBytes object size in bytes
     * @param contentType MIME content type
     */
    void put(String storagePath, InputStream inputStream, long sizeBytes, String contentType);

    /**
     * Moves or renames an object within the same bucket.
     *
     * <p>This operation is performed atomically by the underlying storage provider.
     * The source object is removed after a successful move.</p>
     *
     * @param sourceObject current object key (path) to be moved
     * @param targetObject new object key (path) after move
     * @throws ObjectStorageMoveException if move fails
     */
    void move(String sourceObject, String targetObject);

    /**
     * Deletes object from storage by object key.
     *
     * @param storagePath unique storage object path
     */
    void delete(String storagePath);
}
