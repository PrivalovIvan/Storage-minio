package ru.storage.filestorageservice.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.storage.filestorageservice.config.props.minio.MinioProperties;

@Configuration
@RequiredArgsConstructor
public class MinioClientConfig {
    private final MinioProperties minioProperties;

    @Bean
    MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(minioProperties.endpoint())
            .credentials(
                minioProperties.accessKey(),
                minioProperties.secretKey()
            )
            .build();
    }
}
