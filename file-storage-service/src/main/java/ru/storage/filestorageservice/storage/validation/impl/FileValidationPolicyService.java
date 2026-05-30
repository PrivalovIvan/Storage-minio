package ru.storage.filestorageservice.storage.validation.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.storage.filestorageservice.storage.validation.FileValidationPolicy;
import ru.storage.filestorageservice.storage.validation.context.ValidationContext;
import ru.storage.filestorageservice.storage.validation.validator.FileValidator;

import java.io.InputStream;
import java.util.List;

/**
 * Standard implementation of {@link FileValidationPolicy} using a chain of {@link FileValidator}s.
 * <p>
 * The service receives all Spring-managed validators (e.g. {@code SizeValidator},
 * {@code ExtensionValidator}, {@code ContentTypeValidator}) and executes them sequentially.
 * Each validator operates on a shared {@link ValidationContext} that carries the input stream, filename, and file size.
 * </p>
 * <p>
 * The filename is sanitized for log injection prevention before being passed to validators.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileValidationPolicyService implements FileValidationPolicy {
    private final List<FileValidator> validators;

    @Override
    public void validate(InputStream inputStream, String filename, long sizeBytes) {
        String safeFilename = sanitizeLog(filename);
        log.debug("Starting validation for file: {}", safeFilename);

        ValidationContext context = new ValidationContext(inputStream, safeFilename, sizeBytes);
        validators.forEach(validator -> validator.validate(context));

        log.info("File '{}' passed all validations", safeFilename);
    }

    private String sanitizeLog(String input) {
        return input == null ? null : input.replaceAll("\\p{Cntrl}", "?");
    }
}
