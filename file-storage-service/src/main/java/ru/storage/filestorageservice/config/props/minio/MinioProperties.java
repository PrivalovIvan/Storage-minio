package ru.storage.filestorageservice.config.props.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.minio")
public record MinioProperties(
    String endpoint,
    String accessKey,
    String secretKey,
    String bucket
) {
}
