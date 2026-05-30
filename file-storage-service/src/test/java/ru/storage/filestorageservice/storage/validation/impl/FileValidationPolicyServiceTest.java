package ru.storage.filestorageservice.storage.validation.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.storage.filestorageservice.storage.validation.FileValidationPolicy;
import ru.storage.filestorageservice.storage.validation.exception.FileTooLargeException;
import ru.storage.filestorageservice.storage.validation.exception.FileValidationException;
import ru.storage.filestorageservice.storage.validation.exception.InvalidContentTypeException;
import ru.storage.filestorageservice.storage.validation.exception.InvalidFileExtensionException;
import ru.storage.filestorageservice.storage.validation.exception.MissingFileNameException;
import ru.storage.filestorageservice.storage.validation.props.FileValidationProperties;
import ru.storage.filestorageservice.storage.validation.validator.impl.ContentTypeValidator;
import ru.storage.filestorageservice.storage.validation.validator.impl.ExtensionValidator;
import ru.storage.filestorageservice.storage.validation.validator.impl.FileContentTypeDetector;
import ru.storage.filestorageservice.storage.validation.validator.impl.SizeValidator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileValidationPolicyServiceTest {

    private FileValidationProperties properties;
    private FileValidationPolicy service;
    private FileContentTypeDetector detector;

    private File tempFile;

    @BeforeEach
    void setUp() {
        properties = new FileValidationProperties(
            5 * 1024 * 1024,
            Set.of("pdf", "jpg", "png"),
            Set.of("application/pdf", "image/jpeg", "image/png")
        );
        detector = new FileContentTypeDetector();
        var validators = java.util.List.of(
            new SizeValidator(properties),
            new ExtensionValidator(properties),
            new ContentTypeValidator(properties, detector)
        );
        service = new FileValidationPolicyService(validators);
    }

    @AfterEach
    void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    @DisplayName("Should validate valid PDF file successfully")
    void shouldValidateValidPdfFile() throws Exception {
        tempFile = createValidPdfFile();
        String filename = "document.pdf";
        long size = tempFile.length();

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertDoesNotThrow(() -> service.validate(is, filename, size));
        }
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void shouldThrowExceptionWhenFileIsEmpty() throws IOException {
        tempFile = File.createTempFile("empty", ".pdf");
        tempFile.setReadable(true, true);
        tempFile.setWritable(true, true);

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(FileValidationException.class,
                () -> service.validate(is, "empty.pdf", tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw exception when file too large")
    void shouldThrowExceptionWhenFileTooLarge() throws IOException {
        tempFile = File.createTempFile("large", ".pdf");
        try (OutputStream os = new FileOutputStream(tempFile)) {
            os.write(new byte[6 * 1024 * 1024]);
        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(FileTooLargeException.class,
                () -> service.validate(is, "large.pdf", tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw exception when extension not allowed")
    void shouldThrowExceptionWhenExtensionInvalid() throws Exception {
        tempFile = createValidPdfFile();
        String filename = "document.exe";

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(InvalidFileExtensionException.class,
                () -> service.validate(is, filename, tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw exception when filename has no extension")
    void shouldThrowExceptionWhenFilenameHasNoExtension() throws Exception {
        tempFile = createValidPdfFile();
        String filename = "document";

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(InvalidFileExtensionException.class,
                () -> service.validate(is, filename, tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw exception when filename ends with a dot")
    void shouldThrowExceptionWhenFilenameEndsWithDot() throws Exception {
        tempFile = createValidPdfFile();
        String filename = "document.";

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(InvalidFileExtensionException.class,
                () -> service.validate(is, filename, tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw MissingFileNameException when filename is blank")
    void shouldThrowExceptionWhenFilenameIsBlank() throws Exception {
        tempFile = createValidPdfFile();
        String filename = "   ";

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(MissingFileNameException.class,
                () -> service.validate(is, filename, tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw MissingFileNameException when filename is null")
    void shouldThrowMissingFileNameException() throws Exception {
        tempFile = createValidPdfFile();
        String filename = null;

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(MissingFileNameException.class,
                () -> service.validate(is, filename, tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should throw exception when real content type not allowed")
    void shouldThrowExceptionWhenContentTypeMismatch() throws IOException {
        tempFile = createTextFile();
        String filename = "document.png";

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertThrows(InvalidContentTypeException.class,
                () -> service.validate(is, filename, tempFile.length()));
        }
    }

    @Test
    @DisplayName("Should allow file when extension and real MIME type match allowed list")
    void shouldAllowWhenExtensionAndMimeMatch() throws Exception {
        tempFile = createValidPdfFile();
        String filename = "document.pdf";

        try (InputStream is = new BufferedInputStream(new FileInputStream(tempFile))) {
            assertDoesNotThrow(() -> service.validate(is, filename, tempFile.length()));
        }
    }

    private File createValidPdfFile() throws IOException {
        File file = File.createTempFile("test-", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(file);
        }
        file.setExecutable(false);
        file.setReadable(true, true);
        file.setWritable(true, true);
        return file;
    }

    private File createTextFile() throws IOException {
        File file = File.createTempFile("test-", ".txt");
        try (FileWriter w = new FileWriter(file)) {
            w.write("Hello, world!");
        }
        file.setExecutable(false);
        file.setReadable(true, true);
        file.setWritable(true, true);
        return file;
    }
}
