package ru.storage.filestorageservice.storage.client.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.storage.filestorageservice.config.props.minio.MinioProperties;

/**
 * Ensures that the configured MinIO bucket exists at application startup.
 * If the bucket is missing, it is automatically created.
 * <p>
 * This component implements {@link ApplicationRunner} to perform the check
 * after the Spring context is fully initialized.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketValidator implements ApplicationRunner {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void run(ApplicationArguments args) {
        initializeBucket();
    }

    /**
     * Verifies existence of the configured bucket and creates it if absent.
     *
     * @throws IllegalStateException if bucket existence check or creation fails
     */
    public void initializeBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(minioProperties.bucket())
                    .build()
            );

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.bucket())
                    .build());
                log.info("Created MinIO bucket: {}", minioProperties.bucket());
            }
        } catch (MinioException e) {
            log.error("Failed to ensure MinIO bucket: {}", minioProperties.bucket(), e);
            throw new IllegalStateException("Failed to ensure MinIO bucket: " + minioProperties.bucket(), e);
        }
    }
}
