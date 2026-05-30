package ru.storage.filestorageservice.storage.validation.exception;

// Thrown when original filename is null
public class MissingFileNameException extends FileValidationException {

    public MissingFileNameException(String message) {
        super(message);
    }
}
