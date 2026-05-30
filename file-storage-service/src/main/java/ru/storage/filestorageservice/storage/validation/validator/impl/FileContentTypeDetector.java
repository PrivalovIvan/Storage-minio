package ru.storage.filestorageservice.storage.validation.validator.impl;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Component that detects MIME type of a file from its content.
 * <p>
 * Uses Apache Tika's {@link DefaultDetector} which reads magic bytes
 * from the beginning of the stream. The detector is stateless and thread‑safe.
 * </p>
 */
@Component
public class FileContentTypeDetector {
    private final DefaultDetector detector = new DefaultDetector();

    /**
     * Detects the MIME type of the content provided by the input stream.
     * <p>
     * This method reads the first few bytes of the stream to determine the file type
     * based on magic byte signatures. The stream is not closed by this method.
     * </p>
     *
     * @param inputStream the stream to read from (must support mark/reset or be buffered)
     * @return the detected MIME type as a string (e.g. "application/pdf")
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public String detect(InputStream inputStream) throws IOException {
        var mediaType = detector.detect(inputStream, new Metadata());
        return mediaType.toString();
    }
}
