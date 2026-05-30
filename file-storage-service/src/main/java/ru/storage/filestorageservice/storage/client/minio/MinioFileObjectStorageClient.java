package ru.storage.filestorageservice.storage.client.minio;

import io.minio.CopyObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SourceObject;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.storage.filestorageservice.config.props.minio.MinioProperties;
import ru.storage.filestorageservice.storage.client.FileObjectStorageClient;
import ru.storage.filestorageservice.storage.exception.FileAlreadyExistsException;
import ru.storage.filestorageservice.storage.exception.ObjectStorageDeleteException;
import ru.storage.filestorageservice.storage.exception.ObjectStorageMoveException;
import ru.storage.filestorageservice.storage.exception.ObjectStorageUploadException;

import java.io.InputStream;
import java.util.Map;

/**
 * MinIO-based implementation of {@link FileObjectStorageClient}.
 *
 * <p>Uses MinIO Java SDK for performing object storage operations
 * such as upload, deletion, and existence checks.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MinioFileObjectStorageClient implements FileObjectStorageClient {
    private static final int PRECONDITION_FAILED = 412;
    private static final int DEFAULT_DELAY_MS = 200;

    private final MinioClient client;
    private final MinioProperties properties;

    @Override
    public void put(String storagePath, InputStream inputStream, long sizeBytes, String contentType) {
        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storagePath)
                    .stream(inputStream, sizeBytes, -1L)
                    .headers(Map.of("If-None-Match", "*"))
                    .contentType(contentType)
                    .build()
            );
        } catch (ErrorResponseException e) {
            if (e.response().code() == PRECONDITION_FAILED) {
                throw new FileAlreadyExistsException(storagePath, e);
            }
            throw new ObjectStorageUploadException(storagePath, e);
        } catch (MinioException e) {
            throw new ObjectStorageUploadException(storagePath, e);
        }
    }

    @Override
    public void move(String sourceObject, String targetObject) {
        try {
            client.copyObject(
                CopyObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(targetObject)
                    .source(
                        SourceObject.builder()
                            .bucket(properties.bucket())
                            .object(sourceObject)
                            .build()
                    )
                    .build()
            );
        } catch (MinioException e) {
            throw new ObjectStorageMoveException(properties.bucket(), sourceObject, targetObject, e);
        }
        try {
            deleteWithRetry(sourceObject);
        } catch (ObjectStorageDeleteException e) {
            log.error("Failed to deleteByEventId source object after move: {}", sourceObject, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storagePath)
                    .build()
            );
        } catch (MinioException e) {
            throw new ObjectStorageDeleteException(storagePath, e);
        }
    }

    private void deleteWithRetry(String storagePath) {
        Exception lastException = null;
        long delayMs = DEFAULT_DELAY_MS;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                delete(storagePath);
                return;
            } catch (ObjectStorageDeleteException e) {
                lastException = e;
                if (attempt == 3) break;
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delayMs *= 2;
            }
        }
        log.error("Failed to deleteByEventId after 3 retries: {}", storagePath, lastException);
    }
}
