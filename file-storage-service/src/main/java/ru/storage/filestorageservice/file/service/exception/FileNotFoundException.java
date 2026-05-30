package ru.storage.filestorageservice.file.service.exception;

/**
 * Exception indicating that the requested file does not exist or is not accessible to the current user.
 * <p>
 * Thrown when retrieving or deleting a file by its identifier if the database record is missing
 * or the file belongs to another user.
 * </p>
 */
public class FileNotFoundException extends FileException {
    public FileNotFoundException(String message) {
        super(message);
    }
}
