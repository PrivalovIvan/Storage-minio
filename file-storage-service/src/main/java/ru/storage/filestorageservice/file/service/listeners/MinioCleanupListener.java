package ru.storage.filestorageservice.file.service.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.storage.filestorageservice.file.service.listeners.event.FileUploadRollbackEvent;
import ru.storage.filestorageservice.storage.client.FileObjectStorageClient;
import ru.storage.filestorageservice.storage.exception.ObjectStorageDeleteException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioCleanupListener {
    private final FileObjectStorageClient minioClient;

    /**
     * Handles the rollback of a transaction during file upload.
     * <p>
     * When a transaction is rolled back after a file has been stored in MinIO,
     * this method is invoked to delete the orphaned file from the object storage.
     * The deletion is performed best-effort; failures are logged but do not
     * affect the outcome of the original transaction.
     * </p>
     *
     * @param event the rollback event containing the storage path of the file
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(FileUploadRollbackEvent event) {
        try {
            minioClient.delete(event.storagePath());
            log.debug("Successfully deleted orphaned file from MinIO: {}", event.storagePath());
        } catch (ObjectStorageDeleteException e) {
            log.error("Failed to delete orphaned file from MinIO: {}", event.storagePath(), e);
        }
    }
}
