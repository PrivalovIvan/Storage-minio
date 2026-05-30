package ru.storage.filestorageservice.storage.validation.exception;

// Base exception for all file validation errors
public class FileValidationException extends RuntimeException {

    public FileValidationException(String message) {
        super(message);
    }

    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
