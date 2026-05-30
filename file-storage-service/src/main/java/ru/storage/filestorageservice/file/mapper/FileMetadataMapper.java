package ru.storage.filestorageservice.file.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.storage.contracts.dto.FileUploadResponse;
import ru.storage.filestorageservice.file.service.dto.FileUploadDto;

/**
 * MapStruct mapper for converting internal file DTOs to OpenAPI response models.
 * <p>
 * This mapper is used by the controller layer to transform {@link FileUploadDto}
 * (which is used internally by the service) into {@link FileUploadResponse}
 * (the model defined in the OpenAPI specification). The conversion is stateless
 * and thread‑safe.
 * </p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMetadataMapper {

    /**
     * Converts an internal file upload DTO to an OpenAPI file upload response.
     *
     * @param fileUploadDto the service‑layer DTO containing file metadata
     * @return the response model ready to be sent to the client
     */
    FileUploadResponse toFileUploadResponse(FileUploadDto fileUploadDto);
}
