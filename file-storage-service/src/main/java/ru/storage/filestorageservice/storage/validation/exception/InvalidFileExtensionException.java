package ru.storage.filestorageservice.storage.validation.exception;

// Thrown when file extension is not allowed or missing
public class InvalidFileExtensionException extends FileValidationException {

    public InvalidFileExtensionException(String extension) {
        super("Unsupported file extension: " + extension);
    }
}
