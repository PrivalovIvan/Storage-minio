package ru.storage.filestorageservice.file.service.listeners.event;

public record FileUploadRollbackEvent(
    String storagePath
) {
}
