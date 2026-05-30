package ru.storage.filestorageservice.storage.validation.exception;

// Thrown when file size exceeds configured limit
public class FileTooLargeException extends FileValidationException {

    public FileTooLargeException(long actual, long max) {
        super("File size exceeds limit. Actual: %d, max: %d".formatted(actual, max));
    }
}
