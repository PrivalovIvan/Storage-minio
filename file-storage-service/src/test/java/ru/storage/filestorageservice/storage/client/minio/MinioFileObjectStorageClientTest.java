package ru.storage.filestorageservice.storage.client.minio;

import io.minio.CopyObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.storage.filestorageservice.config.props.minio.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioFileObjectStorageClientTest {

    @Mock
    private MinioClient client;

    @Mock
    private MinioProperties properties;

    private MinioFileObjectStorageClient storageClient;

    @BeforeEach
    void setUp() {
        when(properties.bucket()).thenReturn("files");

        storageClient = new MinioFileObjectStorageClient(client, properties);
    }

    @Test
    void shouldUploadObject() throws Exception {
        // given
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        // when
        storageClient.put("key", stream, 4L, "text/plain");

        // then
        ArgumentCaptor<PutObjectArgs> captor =
            ArgumentCaptor.forClass(PutObjectArgs.class);

        verify(client, times(1)).putObject(captor.capture());

        PutObjectArgs args = captor.getValue();

        assertEquals("files", args.bucket());
        assertEquals("key", args.object());
        assertEquals("text/plain", args.contentType().toString());
    }

    @Test
    void shouldDeleteObject() throws Exception {
        // when
        storageClient.delete("key");

        // then
        ArgumentCaptor<RemoveObjectArgs> captor =
            ArgumentCaptor.forClass(RemoveObjectArgs.class);

        verify(client, times(1)).removeObject(captor.capture());

        RemoveObjectArgs args = captor.getValue();

        assertEquals("files", args.bucket());
        assertEquals("key", args.object());
    }

    @Test
    void shouldMoveObject() throws Exception {
        // given
        String source = "source-key";
        String target = "target-key";

        // when
        storageClient.move(source, target);

        // then
        ArgumentCaptor<CopyObjectArgs> copyCaptor =
            ArgumentCaptor.forClass(CopyObjectArgs.class);

        verify(client, times(1)).copyObject(copyCaptor.capture());

        CopyObjectArgs copyArgs = copyCaptor.getValue();
        assertEquals("files", copyArgs.bucket());
        assertEquals(target, copyArgs.object());
        assertEquals("files", copyArgs.source().bucket());
        assertEquals(source, copyArgs.source().object());

        ArgumentCaptor<RemoveObjectArgs> removeCaptor =
            ArgumentCaptor.forClass(RemoveObjectArgs.class);

        verify(client, times(1)).removeObject(removeCaptor.capture());

        RemoveObjectArgs removeArgs = removeCaptor.getValue();
        assertEquals("files", removeArgs.bucket());
        assertEquals(source, removeArgs.object());
    }
}
