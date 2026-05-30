package ru.storage.filestorageservice.outbox.enums;

/**
 * Status of a deletion outbox event.
 */
public enum DeleteEventStatus {
    /**
     * Event has been created but not yet processed.
     */
    PENDING,

    /**
     * Physical file has been successfully deleted from MinIO.
     */
    COMPLETED,

    /**
     * Processing failed after exhausting all retry attempts.
     */
    FAILED
}
