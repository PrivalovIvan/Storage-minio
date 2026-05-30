package ru.storage.filestorageservice.storage.validation.validator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.storage.filestorageservice.storage.validation.context.ValidationContext;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;
import ru.storage.filestorageservice.storage.validation.exception.InvalidContentTypeException;
import ru.storage.filestorageservice.storage.validation.props.FileValidationProperties;
import ru.storage.filestorageservice.storage.validation.validator.FileValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Validator that detects the real MIME type from the file content using Apache Tika.
 * <p>
 * The detection is performed on the provided {@link InputStream} without closing it.
 * The stream must support {@code mark/reset} (e.g. via {@link java.io.BufferedInputStream}).
 * </p>
 * <p>
 * Rejects MIME types not present in {@link FileValidationProperties#allowedContentTypes()}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentTypeValidator implements FileValidator {
    private final FileValidationProperties validationProperties;
    private final FileContentTypeDetector fileContentTypeDetector;

    @Override
    public void validate(ValidationContext context) {
        String filename = context.filename();
        try {
            String detectedType = fileContentTypeDetector.detect(context.inputStream());
            log.debug("Detected MIME type: {}", detectedType);
            if (!validationProperties.allowedContentTypes().contains(detectedType.toLowerCase(Locale.ROOT))) {

                log.warn("Forbidden MIME type '{}' for file: {}", detectedType, filename);
                throw new InvalidContentTypeException(detectedType);
            }
        } catch (IOException e) {
            log.error("MIME detection failed for file: {}", filename, e);
            throw new FileValidationException("MIME detection failed", e);
        }
    }
}
