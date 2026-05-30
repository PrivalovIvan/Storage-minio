package ru.storage.filestorageservice.storage.client.minio;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.storage.filestorageservice.config.props.minio.MinioProperties;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class MinioFileObjectStorageClientIT {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2024-01-16T16-07-38Z");

    private static MinioClient minioClient;
    private static MinioFileObjectStorageClient storageClient;

    @BeforeAll
    static void setup() {
        minioClient = MinioClient.builder()
            .endpoint(minio.getS3URL())
            .credentials(minio.getUserName(), minio.getPassword())
            .build();

        MinioProperties props = new MinioProperties(null, null, null, "files");

        storageClient = new MinioFileObjectStorageClient(minioClient, props);

        try {
            minioClient.makeBucket(
                io.minio.MakeBucketArgs.builder()
                    .bucket("files")
                    .build()
            );
        } catch (Exception ignored) {
        }
    }

    @Test
    void shouldPutAndDeleteObject() throws Exception {
        String key = "test-key";
        byte[] data = "hello-minio".getBytes();

        // 1. upload
        assertDoesNotThrow(() -> storageClient.put(
            key,
            new ByteArrayInputStream(data),
            data.length,
            "text/plain"
        ));

        // 2. deleteByEventId
        assertDoesNotThrow(() -> storageClient.delete(key));

        // 3. verify deletion (optional, using low-level client)
        assertThrows(Exception.class, () -> minioClient.statObject(
            StatObjectArgs.builder()
                .bucket("files")
                .object(key)
                .build()
        ));
    }
}
