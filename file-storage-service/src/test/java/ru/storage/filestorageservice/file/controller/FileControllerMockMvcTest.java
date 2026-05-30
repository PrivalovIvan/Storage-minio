package ru.storage.filestorageservice.file.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.storage.contracts.api.FilesPublicApi;
import ru.storage.contracts.dto.FileUploadResponse;
import ru.storage.filestorageservice.file.mapper.FileMetadataMapper;
import ru.storage.filestorageservice.file.service.FileService;
import ru.storage.filestorageservice.file.service.dto.FileUploadDto;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerMockMvcTest {

    private final UUID fileId = UUID.randomUUID();
    private final String folderName = "tmp/process-456";
    private final String originalFileName = "document.pdf";
    private final String contentType = "application/pdf";
    private final long fileSize = 1024L;
    private final OffsetDateTime createdAt = OffsetDateTime.now();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private FileMetadataMapper fileMetadataMapper;

    @Test
    @DisplayName("POST /upload – should return 201 Created")
    void shouldUploadFile() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFileName,
            contentType,
            new byte[(int) fileSize]
        );

        FileUploadDto dto = new FileUploadDto(fileId, originalFileName, contentType, fileSize, createdAt);
        FileUploadResponse response = new FileUploadResponse()
            .fileId(fileId)
            .originalFileName(originalFileName)
            .contentType(contentType)
            .sizeBytes(fileSize)
            .createdAt(createdAt);

        given(fileService.upload(any(), eq(folderName))).willReturn(dto);
        given(fileMetadataMapper.toFileUploadResponse(dto)).willReturn(response);

        // when/then
        mockMvc.perform(multipart(FilesPublicApi.PATH_UPLOAD_FILE)
                .file(file)
                .param("folderName", folderName))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileId").value(fileId.toString()))
            .andExpect(jsonPath("$.originalFileName").value(originalFileName))
            .andExpect(jsonPath("$.contentType").value(contentType))
            .andExpect(jsonPath("$.sizeBytes").value(fileSize));
    }

    @Test
    @DisplayName("DELETE /files/{fileId} – should return 204 No Content")
    void shouldDeleteFile() throws Exception {
        // given
        UUID fileId = UUID.randomUUID();

        // when/then
        mockMvc.perform(delete(FilesPublicApi.PATH_DELETE_FILE, fileId))
            .andExpect(status().isNoContent());
    }
}
