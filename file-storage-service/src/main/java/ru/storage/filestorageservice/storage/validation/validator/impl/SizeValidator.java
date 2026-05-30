package ru.storage.filestorageservice.storage.validation.validator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.storage.filestorageservice.storage.validation.context.ValidationContext;
import ru.storage.filestorageservice.storage.validation.exception.FileTooLargeException;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;
import ru.storage.filestorageservice.storage.validation.props.FileValidationProperties;
import ru.storage.filestorageservice.storage.validation.validator.FileValidator;

/**
 * Validator that checks file size constraints.
 * <ul>
 *   <li>Rejects empty files (size == 0).</li>
 *   <li>Rejects files exceeding {@link FileValidationProperties#maxSizeBytes()}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SizeValidator implements FileValidator {
    private final FileValidationProperties validationProperties;

    @Override
    public void validate(ValidationContext context) {
        long sizeBytes = context.sizeBytes();
        log.debug("File size: {} bytes", sizeBytes);

        if (sizeBytes == 0) {
            log.warn("Empty file rejected");
            throw new FileValidationException("Empty file is not allowed");
        }

        if (sizeBytes > validationProperties.maxSizeBytes()) {
            log.warn("File too large: {} bytes (max {} bytes)", sizeBytes, validationProperties.maxSizeBytes());
            throw new FileTooLargeException(sizeBytes, validationProperties.maxSizeBytes());
        }
    }
}
