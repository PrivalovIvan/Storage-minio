package ru.storage.filestorageservice.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.storage.filestorageservice.outbox.enums.DeleteEventStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an outbox event for asynchronous deletion of a file from MinIO.
 * <p>
 * When a file is logically deleted (marked as `deleted = true`), a corresponding
 * {@code DeleteEventEntity} is persisted with status {@code PENDING}. A background worker
 * periodically picks up pending events and attempts to delete the physical file.
 * </p>
 *
 * @see DeleteEventStatus
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "delete_event", schema = "file_storage")
public class DeleteEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Identifier of the file to be deleted.
     */
    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    /**
     * Owner of the file (used for audit and potential recovery).
     */
    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    /**
     * Full storage path of the file in storage.
     */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    /**
     * Current processing status of the event.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeleteEventStatus status;

    /**
     * Timestamp when the event was created (set by database default).
     */
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    /**
     * Timestamp when the event was last updated (set by database trigger).
     */
    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private Instant updatedAt;

    /**
     * Optimistic locking version.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Factory method to create a new pending deletion event.
     *
     * @param fileId      identifier of the file to delete
     * @param ownerUserId owner of the file
     * @param storagePath MinIO object key
     * @return a new {@code DeleteEventEntity} with status {@code PENDING}
     */
    public static DeleteEventEntity pending(
        UUID fileId,
        String ownerUserId,
        String storagePath
    ) {
        DeleteEventEntity entity = new DeleteEventEntity();
        entity.fileId = fileId;
        entity.ownerUserId = ownerUserId;
        entity.storagePath = storagePath;
        entity.status = DeleteEventStatus.PENDING;
        return entity;
    }
}
