package ru.storage.filestorageservice.storage.validation.validator;

import ru.storage.filestorageservice.storage.validation.context.ValidationContext;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;

/**
 * Single validation rule in the chain.
 * <p>
 * Implementations should focus on one specific aspect (size, extension, content type, etc.)
 * and throw a dedicated exception (e.g. {@code FileTooLargeException}) on failure.
 * </p>
 */
public interface FileValidator {

    /**
     * Performs the validation on the given context.
     *
     * @param context contains the input stream, filename, and file size
     * @throws FileValidationException if the validation rule is violated
     */
    void validate(ValidationContext context);
}
