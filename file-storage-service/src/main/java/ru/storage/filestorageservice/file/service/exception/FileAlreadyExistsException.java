package ru.storage.filestorageservice.file.service.exception;

public class FileAlreadyExistsException extends FileException {
    public FileAlreadyExistsException(String message) {
        super(message);
    }
}
