package ru.storage.filestorageservice.file.service.dto;


import ru.storage.filestorageservice.file.entity.FileMetadataEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Data Transfer Object for file upload response.
 * <p>
 * Contains the essential metadata of an uploaded file, used to communicate
 * with clients after a successful upload.
 * </p>
 *
 * @param fileId          unique identifier of the uploaded file
 * @param originalFileName original name of the file as provided by the client
 * @param contentType     MIME type of the file
 * @param sizeBytes       file size in bytes
 * @param createdAt       upload timestamp in UTC
 */
public record FileUploadDto(
    UUID fileId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    OffsetDateTime createdAt
) {

    /**
     * Factory method to create a {@code FileUploadDto} from a {@link FileMetadataEntity}.
     *
     * @param entity the file metadata entity (must not be {@code null})
     * @return a new {@code FileUploadDto} instance with data extracted from the entity
     */
    public static FileUploadDto of(FileMetadataEntity entity) {
        return new FileUploadDto(
            entity.getId(),
            entity.getOriginalFileName(),
            entity.getContentType(),
            entity.getSizeBytes(),
            entity.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
