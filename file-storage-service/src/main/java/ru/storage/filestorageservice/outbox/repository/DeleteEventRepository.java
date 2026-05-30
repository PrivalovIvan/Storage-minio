package ru.storage.filestorageservice.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.storage.filestorageservice.outbox.entity.DeleteEventEntity;
import ru.storage.filestorageservice.outbox.enums.DeleteEventStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link DeleteEventEntity}.
 * <p>
 * Provides a method to fetch a batch of pending events with row‑level locking
 * to avoid concurrent processing by multiple worker instances.
 * </p>
 */
public interface DeleteEventRepository extends JpaRepository<DeleteEventEntity, UUID> {

    /**
     * Retrieves a limited batch of pending events for processing.
     * <p>
     * The query uses {@code FOR UPDATE SKIP LOCKED} to ensure that each event
     * is processed by only one worker instance.
     * </p>
     *
     * @param status status of the events to fetch (should be {@code PENDING})
     * @param limit  maximum number of events to return
     * @return a list of pending events, ordered by creation time
     */
    @Query(value = """
        select *
        from file_storage.delete_event
        where status = :status
        order by created_at
        limit :limit
        for update skip locked
        """, nativeQuery = true)
    List<DeleteEventEntity> findBatchForProcessing(
        @Param("status") String status,
        @Param("limit") int limit
    );

    /**
     * Atomically updates the status of a deletion event if it is still pending.
     * <p>
     * This method is used to prevent race conditions when multiple scheduler instances
     * might attempt to process the same event concurrently. The update only succeeds
     * when the event's current status is {@code PENDING}.
     * </p>
     *
     * @param id     the identifier of the deletion event
     * @param status the new status to set (typically {@link DeleteEventStatus#COMPLETED}
     *               or {@link DeleteEventStatus#FAILED})
     * @return the number of rows updated (1 if successful, 0 if the event was already
     *         processed or does not exist in the PENDING state)
     */
    @Modifying
    @Query("UPDATE DeleteEventEntity e SET e.status = :status WHERE e.id = :id AND e.status = 'PENDING'")
    int setStatusIfPending(
        @Param("id") UUID id,
        @Param("status") DeleteEventStatus status
    );
}
