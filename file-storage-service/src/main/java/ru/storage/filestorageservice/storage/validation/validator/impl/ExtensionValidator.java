package ru.storage.filestorageservice.storage.validation.validator.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import ru.storage.filestorageservice.storage.validation.context.ValidationContext;
import ru.storage.filestorageservice.storage.validation.exception.InvalidFileExtensionException;
import ru.storage.filestorageservice.storage.validation.exception.MissingFileNameException;
import ru.storage.filestorageservice.storage.validation.props.FileValidationProperties;
import ru.storage.filestorageservice.storage.validation.validator.FileValidator;

import java.util.Locale;

/**
 * Validator that checks the file extension extracted from the original filename.
 * <p>
 * Uses Apache Commons IO {@link FilenameUtils#getExtension(String)} to reliably extract
 * the extension, even from paths (e.g. "folder/file.txt" → "txt").
 * </p>
 * <ul>
 *   <li>Rejects null or blank filenames (throws {@code MissingFileNameException}).</li>
 *   <li>Rejects filenames ending with a dot.</li>
 *   <li>Rejects extensions not present in {@link FileValidationProperties#allowedExtensions()}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtensionValidator implements FileValidator {
    private final FileValidationProperties validationProperties;

    @Override
    public void validate(ValidationContext context) {
        String filename = context.filename();
        String extension = extractExtension(filename);

        if (!validationProperties.allowedExtensions().contains(extension)) {
            if (extension.isEmpty()) {
                throw new InvalidFileExtensionException("missing file extension");
            }

            log.warn("Unsupported extension '{}' for file: {}", extension, filename);
            throw new InvalidFileExtensionException(extension);
        }

        log.debug("Extension '{}' is allowed", extension);
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            log.error("Filename is null");
            throw new MissingFileNameException("Missing filename");
        }
        if (filename.isBlank()) {
            log.warn("Filename is empty or blank");
            throw new MissingFileNameException("empty filename: " + filename);
        }

        String extension = FilenameUtils.getExtension(filename);

        if (extension.isEmpty() && filename.trim().endsWith(".")) {
            log.warn("Filename ends with a dot: {}", filename);
            throw new InvalidFileExtensionException("filename ends with a dot: " + filename);
        }

        return extension.toLowerCase(Locale.ROOT);
    }
}
