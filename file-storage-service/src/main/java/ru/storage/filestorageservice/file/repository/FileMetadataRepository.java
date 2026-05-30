package ru.storage.filestorageservice.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.storage.filestorageservice.file.entity.FileMetadataEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link FileMetadataEntity} persistence operations.
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, UUID> {

    /**
     * Finds file metadata by file identifier, owner user identifier and IsDeleted status.
     *
     * @param id file identifier
     * @param ownerUserId owner user identifier
     * @return optional containing file metadata if found
     */
    Optional<FileMetadataEntity> findByIdAndOwnerUserIdAndIsDeletedFalse(UUID id, String ownerUserId);

    /**
     * Checks whether an active (not soft-deleted) file exists for the given folder name and owner.
     *
     * @param ownerUserId identifier of the file owner
     * @param folderName folder name
     * @param originalFileName file name
     * @return {@code true} if an active file with the specified folder name and owner exists, {@code false} otherwise
     */
    boolean existsByOwnerUserIdAndFolderNameAndOriginalFileNameAndIsDeletedFalse(
        String ownerUserId,
        String folderName,
        String originalFileName
    );
}
