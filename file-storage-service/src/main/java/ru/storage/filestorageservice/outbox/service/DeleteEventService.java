package ru.storage.filestorageservice.outbox.service;

import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.storage.filestorageservice.outbox.entity.DeleteEventEntity;
import ru.storage.filestorageservice.outbox.enums.DeleteEventStatus;
import ru.storage.filestorageservice.outbox.repository.DeleteEventRepository;
import ru.storage.filestorageservice.storage.client.FileObjectStorageClient;
import ru.storage.filestorageservice.storage.exception.ObjectStorageDeleteException;

import java.util.UUID;

/**
 * Service responsible for processing a single deletion outbox event.
 * <p>
 * The method {@link #deleteByEventId(UUID)} attempts to delete the physical file
 * from MinIO. It uses Spring Retry to automatically retry on temporary failures
 * (network issues, timeouts) with exponential backoff.
 * </p>
 * <ul>
 *   <li>On success → the event status is atomically updated to {@code COMPLETED}
 *       via {@link DeleteEventRepository#setStatusIfPending(UUID, DeleteEventStatus)}.</li>
 *   <li>If the file is already missing (404) → the event is also marked as
 *       {@code COMPLETED} (considered success).</li>
 *   <li>If the maximum number of retries is exhausted, the exception is propagated
 *       and the event remains {@code PENDING} (will be retried on the next
 *       dispatcher run).</li>
 * </ul>
 *
 * @see Retryable
 * @see DeleteEventRepository#setStatusIfPending(UUID, DeleteEventStatus)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteEventService {
    private static final int HTTP_NOT_FOUND = 404;

    private final DeleteEventRepository repository;
    private final FileObjectStorageClient minioClient;

    /**
     * Deletes the physical file from MinIO for the given deletion event.
     * <p>
     * This method is retryable: on {@link ObjectStorageDeleteException} (except 404)
     * it will be retried up to 5 times with exponential backoff (200ms initial delay,
     * multiplier 2). After successful deletion (or 404) the event status is atomically
     * updated to {@link DeleteEventStatus#COMPLETED}.
     * </p>
     *
     * @param eventId the identifier of the deletion event (must exist in DB)
     * @throws IllegalArgumentException if the event is not found
     * @throws ObjectStorageDeleteException after exhausting all retry attempts
     *         (the event remains {@code PENDING})
     */
    @Retryable(
        value = ObjectStorageDeleteException.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 200, multiplier = 2)
    )
    @Transactional
    public void deleteByEventId(UUID eventId) {
        DeleteEventEntity event = repository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Delete event not found: " + eventId));

        try {
            minioClient.delete(event.getStoragePath());
            int updated = repository.setStatusIfPending(event.getId(), DeleteEventStatus.COMPLETED);
            if (updated == 0) {
                log.warn("Event {} was already completed concurrently", eventId);
            } else {
                log.info("Successfully deleted file {} and completed event {}", event.getStoragePath(), eventId);
            }
        } catch (ObjectStorageDeleteException e) {
            if (isFileNotFoundError(e)) {
                int updated = repository.setStatusIfPending(event.getId(), DeleteEventStatus.COMPLETED);
                if (updated > 0) {
                    log.warn("File {} already missing, completed event {}", event.getStoragePath(), eventId);
                } else {
                    log.debug("Event {} already completed", eventId);
                }
                return;
            }

            log.warn("Temporary failure deleting file {}, will retry", event.getStoragePath());
            throw e;
        }
    }

    /**
     * Recovery method invoked when all retry attempts for a deletion event have been exhausted.
     * <p>
     * This method attempts to atomically transition the event from {@code PENDING} to {@code FAILED}.
     * If the update is successful, the event will no longer be picked up by the dispatcher.
     * If the event is already in a non‑PENDING state (e.g. already completed or failed), nothing is changed.
     * </p>
     *
     * @param e       the last {@link ObjectStorageDeleteException} thrown during retries (cause of the failure)
     * @param eventId the identifier of the deletion event that failed permanently
     */
    @Recover
    public void recover(ObjectStorageDeleteException e, UUID eventId) {
        int updated = repository.setStatusIfPending(eventId, DeleteEventStatus.FAILED);
        if (updated > 0) {
            log.error("Permanent failure: event {} marked as FAILED after {} retries", eventId, 5, e);
        } else {
            log.warn("Event {} already not in PENDING state, skipping recovery", eventId);
        }
    }

    /**
     * Determines whether the exception indicates that the object does not exist in MinIO.
     *
     * @param e the exception thrown by the storage client
     * @return {@code true} if the error is a 404 Not Found
     */
    private boolean isFileNotFoundError(ObjectStorageDeleteException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ErrorResponseException ex) {
            return ex.response().code() == HTTP_NOT_FOUND;
        }
        return false;
    }
}
