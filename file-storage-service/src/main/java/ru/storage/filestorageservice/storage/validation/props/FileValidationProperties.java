package ru.storage.filestorageservice.storage.validation.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration properties for file validation.
 * Loads settings from {@code file-storage.validation} prefix in application YAML.
 *
 * @param maxSizeBytes        maximum allowed file size in bytes
 * @param allowedExtensions   case-insensitive set of permitted file extensions
 * @param allowedContentTypes case-insensitive set of permitted MIME types
 */
@ConfigurationProperties(prefix = "file-storage.validation")
public record FileValidationProperties(
    long maxSizeBytes,
    Set<String> allowedExtensions,
    Set<String> allowedContentTypes) {

    public FileValidationProperties {

        allowedExtensions = allowedExtensions.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toUnmodifiableSet());

        allowedContentTypes = allowedContentTypes.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toUnmodifiableSet());
    }
}
