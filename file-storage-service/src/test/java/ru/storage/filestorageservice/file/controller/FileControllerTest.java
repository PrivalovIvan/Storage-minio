package ru.storage.filestorageservice.file.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.storage.contracts.dto.FileUploadResponse;
import ru.storage.filestorageservice.file.mapper.FileMetadataMapper;
import ru.storage.filestorageservice.file.service.FileService;
import ru.storage.filestorageservice.file.service.dto.FileUploadDto;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private final UUID fileId = UUID.randomUUID();
    private final String folderName = "tmp/process-456";
    private final String originalFileName = "document.pdf";
    private final String contentType = "application/pdf";
    private final long fileSize = 1024L;
    private final OffsetDateTime createdAt = OffsetDateTime.now();
    @Mock
    private FileService fileService;
    @Mock
    private FileMetadataMapper fileMetadataMapper;
    @InjectMocks
    private FileController fileController;

    @Test
    @DisplayName("Should upload file and return 201 Created with response body")
    void shouldUploadFileAndReturnCreated() {
        // given
        MultipartFile file = new MockMultipartFile(
            "file",
            originalFileName,
            contentType,
            new byte[(int) fileSize]
        );

        FileUploadDto dto = new FileUploadDto(
            fileId,
            originalFileName,
            contentType,
            fileSize,
            createdAt
        );

        FileUploadResponse response = new FileUploadResponse()
            .fileId(fileId)
            .originalFileName(originalFileName)
            .contentType(contentType)
            .sizeBytes(fileSize)
            .createdAt(createdAt);

        given(fileService.upload(file, folderName)).willReturn(dto);
        given(fileMetadataMapper.toFileUploadResponse(dto)).willReturn(response);

        // when
        ResponseEntity<FileUploadResponse> result = fileController.uploadFile(file, folderName);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getFileId()).isEqualTo(fileId);
        assertThat(result.getBody().getOriginalFileName()).isEqualTo(originalFileName);
        assertThat(result.getBody().getContentType()).isEqualTo(contentType);
        assertThat(result.getBody().getSizeBytes()).isEqualTo(fileSize);
        assertThat(result.getBody().getCreatedAt()).isEqualTo(createdAt);

        verify(fileService).upload(file, folderName);
        verify(fileMetadataMapper).toFileUploadResponse(dto);
    }

    @Test
    @DisplayName("Should deleteByEventId file and return 204 No Content")
    void shouldDeleteFileAndReturnNoContent() {
        // given
        UUID fileId = UUID.randomUUID();

        // when
        ResponseEntity<Void> result = fileController.deleteFile(fileId);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();

        verify(fileService).delete(fileId);
    }
}
