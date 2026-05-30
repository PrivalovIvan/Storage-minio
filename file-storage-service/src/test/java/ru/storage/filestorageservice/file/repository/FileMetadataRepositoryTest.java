package ru.storage.filestorageservice.file.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.storage.filestorageservice.db.AbstractRepositoryIntegrationWithLiquibase;
import ru.storage.filestorageservice.file.entity.FileMetadataEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileMetadataRepositoryTest extends AbstractRepositoryIntegrationWithLiquibase {

    @Autowired
    private FileMetadataRepository repository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
    }

    @Test
    void shouldSaveEntityWithInstant() {
        // given
        FileMetadataEntity entity = createEntity();

        // when
        FileMetadataEntity saved = repository.saveAndFlush(entity);

        entityManager.clear();

        FileMetadataEntity actual =
            repository.findById(saved.getId()).orElseThrow();

        // then
        assertNotNull(actual.getId());
        assertNotNull(actual.getCreatedAt());
        assertNotNull(actual.getUpdatedAt());
    }

    @Test
    @DisplayName("Should return FileMetadataEntity when id and ownerUserId present")
    void findByIdAndOwnerUserId_shouldReturnFileMetadataEntity_whenIdAndOwnerUserIdPresent() {
        // given
        FileMetadataEntity entity = createEntity();

        repository.saveAndFlush(entity);

        // when
        Optional<FileMetadataEntity> result =
            repository.findByIdAndOwnerUserIdAndIsDeletedFalse(entity.getId(), "user");

        // then
        FileMetadataEntity actual = result.orElseThrow();

        assertEquals(entity.getId(), actual.getId());
        assertEquals(entity.getOwnerUserId(), actual.getOwnerUserId());
    }

    @Test
    @DisplayName("Should return empty when ownerUserId not found")
    void findByIdAndOwnerUserId_shouldReturnEmpty_whenOwnerUserIdNotFound() {
        // given
        FileMetadataEntity entity = createEntity();

        repository.saveAndFlush(entity);

        // when
        Optional<FileMetadataEntity> result =
            repository.findByIdAndOwnerUserIdAndIsDeletedFalse(entity.getId(), "not-present");

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when Id not found")
    void findByIdAndOwnerUserId_shouldReturnEmpty_whenIdNotFound() {
        // given
        FileMetadataEntity entity = createEntity();

        repository.saveAndFlush(entity);

        // when
        Optional<FileMetadataEntity> result =
            repository.findByIdAndOwnerUserIdAndIsDeletedFalse(
                UUID.randomUUID(),
                entity.getOwnerUserId()
            );

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return list FileMetadataEntity when ids exist")
    void findAllById_shouldReturnEntities_whenIdsExist() {
        // given
        FileMetadataEntity entity1 = createEntity();
        FileMetadataEntity entity2 = createEntity();
        FileMetadataEntity entity3 = createEntity();

        repository.saveAllAndFlush(List.of(entity1, entity2, entity3));

        // when
        List<FileMetadataEntity> result =
            repository.findAllById(
                List.of(
                    entity1.getId(),
                    entity2.getId(),
                    entity3.getId()
                )
            );

        // then
        assertFalse(result.isEmpty());
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Should return empty list when id not found")
    void findAllById_shouldReturnEmptyList_whenIdNotPresent() {
        // when
        List<FileMetadataEntity> result =
            repository.findAllById(List.of(UUID.randomUUID()));

        // then
        assertTrue(result.isEmpty());
    }

    private FileMetadataEntity createEntity() {
        return new FileMetadataEntity(
            "user",
            "file.pdf",
            "folder",
            "application/pdf",
            "pdf",
            100L,
            UUID.randomUUID().toString(),
            null
        );
    }
}
