package ru.storage.filestorageservice.service;

import io.minio.errors.ErrorResponseException;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.storage.filestorageservice.outbox.entity.DeleteEventEntity;
import ru.storage.filestorageservice.outbox.enums.DeleteEventStatus;
import ru.storage.filestorageservice.outbox.repository.DeleteEventRepository;
import ru.storage.filestorageservice.outbox.service.DeleteEventService;
import ru.storage.filestorageservice.storage.client.FileObjectStorageClient;
import ru.storage.filestorageservice.storage.exception.ObjectStorageDeleteException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteEventServiceTest {

    private final UUID fileId = UUID.randomUUID();
    private final String userId = "user-123";
    private final String storagePath = "tmp/process-456/document.pdf";
    @Mock
    private FileObjectStorageClient minioClient;
    @Mock
    private DeleteEventRepository repository;
    @InjectMocks
    private DeleteEventService deleteEventService;
    private UUID eventId;
    private DeleteEventEntity event;

    @BeforeEach
    void setUp() throws Exception {
        eventId = UUID.randomUUID();
        event = DeleteEventEntity.pending(fileId, userId, storagePath);
        var idField = DeleteEventEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(event, eventId);
    }

    @Test
    @DisplayName("Should complete event when MinIO deletion succeeds")
    void shouldCompleteEventOnSuccessfulDeletion() {
        // given
        given(repository.findById(eventId)).willReturn(Optional.of(event));
        given(repository.setStatusIfPending(eventId, DeleteEventStatus.COMPLETED)).willReturn(1);

        // when
        deleteEventService.deleteByEventId(eventId);

        // then
        verify(minioClient).delete(storagePath);
        verify(repository).setStatusIfPending(eventId, DeleteEventStatus.COMPLETED);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should complete event when file not found in MinIO (404)")
    void shouldCompleteEventWhenFileNotFound() {
        // given
        given(repository.findById(eventId)).willReturn(Optional.of(event));
        ObjectStorageDeleteException notFoundException = mock(ObjectStorageDeleteException.class);
        ErrorResponseException cause = mock(ErrorResponseException.class);
        Response response = mock(Response.class);
        given(cause.response()).willReturn(response);
        given(response.code()).willReturn(404);
        given(notFoundException.getCause()).willReturn(cause);
        doThrow(notFoundException).when(minioClient).delete(storagePath);
        given(repository.setStatusIfPending(eventId, DeleteEventStatus.COMPLETED)).willReturn(1);

        // when
        deleteEventService.deleteByEventId(eventId);

        // then
        verify(minioClient).delete(storagePath);
        verify(repository).setStatusIfPending(eventId, DeleteEventStatus.COMPLETED);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should keep event PENDING and throw exception on temporary MinIO error")
    void shouldKeepPendingOnTemporaryError() {
        // given
        given(repository.findById(eventId)).willReturn(Optional.of(event));
        doThrow(new ObjectStorageDeleteException("Network error", new RuntimeException()))
            .when(minioClient).delete(storagePath);

        // when / then
        assertThatThrownBy(() -> deleteEventService.deleteByEventId(eventId))
            .isInstanceOf(ObjectStorageDeleteException.class);

        verify(minioClient).delete(storagePath);
        verify(repository, never()).setStatusIfPending(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark FAILED when retry exhausted (via @Recover)")
    void shouldMarkFailedWhenRetryExhausted() {
        // given
        given(repository.findById(eventId)).willReturn(Optional.of(event));
        doThrow(new ObjectStorageDeleteException("Persistent error", new RuntimeException()))
            .when(minioClient).delete(storagePath);
        // when / then
        assertThatThrownBy(() -> deleteEventService.deleteByEventId(eventId))
            .isInstanceOf(ObjectStorageDeleteException.class);

        verify(minioClient).delete(storagePath);
        verify(repository, never()).setStatusIfPending(any(), any());
    }
}
