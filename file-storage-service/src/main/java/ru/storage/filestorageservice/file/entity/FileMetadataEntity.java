package ru.storage.filestorageservice.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.storage.filestorageservice.file.service.context.FileContext;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing file metadata stored in the system.
 * <p>
 * Binary content is stored externally in MinIO (or another object storage),
 * while this entity contains only metadata such as file name, size, content type,
 * storage path, and ownership information. Soft deletion is supported via the
 * {@code is_deleted} flag, and optimistic locking is enabled via the {@code version} field.
 * </p>
 *
 * @see FileContext
 */
@Entity
@Table(schema = "file_storage", name = "files")
@Getter
@NoArgsConstructor
public class FileMetadataEntity {

    /**
     * Unique file identifier (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Identifier of the file owner (user ID).
     */
    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    /**
     * Original file name as provided by the user.
     */
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    /**
     * Logical folder name (user‑friendly) where the file is stored.
     */
    @Column(name = "folder_name", nullable = false)
    private String folderName;

    /**
     * MIME content type of the file (e.g. "application/pdf").
     */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /**
     * File extension (e.g. "pdf", "jpg"). May be empty if no extension.
     */
    @Column(name = "extension", nullable = false)
    private String extension;

    /**
     * File size in bytes.
     */
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /**
     * Unique object key (path) in the object storage (MinIO).
     */
    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

    /**
     * SHA‑256 checksum of the file content (optional, may be {@code null}).
     */
    @Column(name = "checksum_sha256")
    private String checksumSha256;

    /**
     * File creation timestamp (set by the database).
     */
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    /**
     * Last file metadata update timestamp (set by the database trigger).
     */
    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private Instant updatedAt;

    /**
     * Soft‑delete flag: {@code true} if the file has been marked as deleted,
     * {@code false} otherwise.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    /**
     * Optimistic locking version to prevent concurrent modification conflicts.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Constructor for creating a new file metadata entity.
     *
     * @param ownerUserId     identifier of the file owner
     * @param originalFileName original file name
     * @param folderName      logical folder name
     * @param contentType     MIME type
     * @param extension       file extension
     * @param sizeBytes       file size in bytes
     * @param storagePath     unique object key in storage
     * @param checksumSha256  optional SHA‑256 checksum (may be {@code null})
     */
    public FileMetadataEntity(
        String ownerUserId,
        String originalFileName,
        String folderName,
        String contentType,
        String extension,
        Long sizeBytes,
        String storagePath,
        String checksumSha256
    ) {
        this.ownerUserId = ownerUserId;
        this.originalFileName = originalFileName;
        this.folderName = folderName;
        this.contentType = contentType;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
        this.checksumSha256 = checksumSha256;
    }

    /**
     * Factory method to create a {@code FileMetadataEntity} from a pre‑processed
     * {@link FileContext}. The context already contains all necessary metadata
     * (user, sanitised filename, extension, storage path, etc.).
     *
     * @param context the pre‑processed file context (cannot be {@code null})
     * @return a new {@code FileMetadataEntity} instance (without checksum)
     */
    public static FileMetadataEntity of(FileContext context) {
        return new FileMetadataEntity(
            context.userId(),
            context.filename(),
            context.folderName(),
            context.contentType(),
            context.extension(),
            context.sizeBytes(),
            context.storagePath(),
            null
        );
    }

    /**
     * Marks the file as logically deleted (soft delete). The actual binary content
     * will be removed asynchronously by the outbox worker.
     */
    public void markDeleted() {
        this.isDeleted = true;
    }
}
