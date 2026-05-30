package ru.storage.filestorageservice.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.storage.contracts.dto.ErrorResponse;
import ru.storage.filestorageservice.common.security.exception.AuthenticationException;
import ru.storage.filestorageservice.file.service.exception.FileAlreadyExistsException;
import ru.storage.filestorageservice.file.service.exception.FileException;
import ru.storage.filestorageservice.file.service.exception.FileNotFoundException;
import ru.storage.filestorageservice.storage.exception.ObjectStorageException;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;

import java.time.OffsetDateTime;

/**
 * Global exception handler for REST API endpoints.
 * <p>
 * Converts various exceptions into a consistent {@link ErrorResponse} structure
 * with appropriate HTTP status codes. Logs each exception with a suitable log level.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link AuthenticationException} – missing or invalid authentication.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 401 Unauthorized error response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException ex,
        HttpServletRequest request) {
        log.warn("Authentication error: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    /**
     * Handles {@link FileValidationException} – file validation errors (size, extension, MIME type).
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 400 Bad Request error response
     */
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidationException(
        FileValidationException ex,
        HttpServletRequest request) {
        log.warn("File validation failed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles {@link FileAlreadyExistsException} – attempt to upload a file that already exists.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 409 Conflict error response
     */
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleFileAlreadyExists(
        FileAlreadyExistsException ex,
        HttpServletRequest request
    ) {
        log.warn(ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    /**
     * Handles {@link FileNotFoundException} – requested file does not exist.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 404 Not Found error response
     */
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex, HttpServletRequest request) {
        log.warn("File not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles general {@link FileException} – any file service related error not covered by more specific handlers.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(
        FileException ex,
        HttpServletRequest request) {
        log.error("File error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles {@link ObjectStorageException} – errors from MinIO or other object storage.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(ObjectStorageException.class)
    public ResponseEntity<ErrorResponse> handleObjectStorageException(
        ObjectStorageException ex,
        HttpServletRequest request) {
        log.error("Object storage error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles {@link NotImplementedException} – endpoint not yet implemented.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 501 Not Implemented
     */
    @ExceptionHandler(NotImplementedException.class)
    public ResponseEntity<ErrorResponse> handleNotImplemented(NotImplementedException ex, HttpServletRequest request) {
        log.warn("Endpoint not implemented: {}", request.getRequestURI());
        return buildErrorResponse(ex, HttpStatus.NOT_IMPLEMENTED, request);
    }

    /**
     * Handles {@link OptimisticLockingFailureException} – concurrent modification conflict.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 409 Conflict
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
        OptimisticLockingFailureException ex,
        HttpServletRequest request) {
        log.warn("Optimistic lock failure: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    /**
     * Handles {@link MissingServletRequestParameterException} – required request parameter missing.
     *
     * @param ex      the exception
     * @param request the current HTTP request
     * @return 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Builds a uniform {@link ErrorResponse} object from the given exception and request data.
     *
     * @param ex      the thrown exception
     * @param status  the HTTP status to return
     * @param request the current HTTP request (provides method and URI)
     * @return a populated {@link ErrorResponse}
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
        Exception ex,
        HttpStatus status,
        HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            ex.getMessage(),
            request.getMethod(),
            request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
