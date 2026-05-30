package ru.storage.filestorageservice.file.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.storage.contracts.api.FilesPublicApi;
import ru.storage.contracts.dto.FileUploadResponse;
import ru.storage.filestorageservice.file.mapper.FileMetadataMapper;
import ru.storage.filestorageservice.file.service.FileService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController implements FilesPublicApi {
    private final FileService fileService;
    private final FileMetadataMapper fileMetadataMapper;

    @Override
    public ResponseEntity<Void> deleteFile(@NotNull UUID fileId) {
        fileService.delete(fileId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<FileUploadResponse> uploadFile(MultipartFile file, @Size(min = 1) @Valid String folderName) {
        var fileMetadataDto = fileService.upload(file, folderName);
        var response = fileMetadataMapper.toFileUploadResponse(fileMetadataDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
