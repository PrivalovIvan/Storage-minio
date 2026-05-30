package ru.storage.filestorageservice.file.service.context;

import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.storage.filestorageservice.common.security.CurrentUserProvider;
import ru.storage.filestorageservice.common.security.exception.AuthenticationException;

import java.util.UUID;

/**
 * Immutable container for preprocessed file upload data.
 * <p>
 * This record encapsulates all the necessary information about an uploaded file
 * after resolving the current user, sanitising the filename, extracting the extension,
 * and generating a storage path. It is used to pass data from the service layer
 * to downstream components without repeating the same logic.
 * </p>
 *
 * @param userId       identifier of the authenticated user (owner of the file)
 * @param filename     sanitised original filename (without path components)
 * @param extension    file extension (lowercase, may be empty)
 * @param storagePath  unique path in the object storage (e.g. "tmp/processId/filename")
 * @param contentType  MIME type provided by the client (validated later)
 * @param sizeBytes    actual file size in bytes
 */
public record FileContext(
    String userId,
    String filename,
    String folderName,
    String extension,
    String storagePath,
    String contentType,
    long sizeBytes
) {

    /**
     * Factory method to build a {@code FileContext} from a multipart file and a temporary folder identifier.
     *
     * @param userProvider       provides the current authenticated user ID
     * @param file               the uploaded multipart file
     * @param folderName  logical folder identifier (e.g. process ID) used to build the storage path
     * @return a fully populated {@code FileContext}
     * @throws AuthenticationException if no authenticated user is found
     */
    public static FileContext of(CurrentUserProvider userProvider, MultipartFile file, String folderName) {
        String userId = userProvider.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException("No authenticated user found"));
        String filename = FilenameUtils.getName(file.getOriginalFilename());
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String storagePath = generateStoragePath(folderName, filename);
        String contentType = file.getContentType();
        long sizeBytes = file.getSize();

        return new FileContext(
            userId,
            filename,
            folderName,
            extension,
            storagePath,
            contentType,
            sizeBytes
        );
    }

    private static String generateStoragePath(String folderName, String filename) {
        return folderName + "/" + UUID.randomUUID() + "/" + filename;
    }
}
