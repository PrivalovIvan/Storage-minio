package ru.storage.filestorageservice.storage.validation.exception;

// Thrown when detected MIME type is not allowed
public class InvalidContentTypeException extends FileValidationException {

    public InvalidContentTypeException(String contentType) {
        super("Unsupported content type: " + contentType);
    }
}
