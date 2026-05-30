package ru.storage.filestorageservice.storage.validation.context;

import java.io.InputStream;

/**
 * Immutable container for data shared across all validators in the chain.
 *
 * @param inputStream stream of the file content (already positioned at the beginning)
 * @param filename    original client‑provided filename (sanitized for logs)
 * @param sizeBytes   actual file size in bytes
 */
public record ValidationContext(
    InputStream inputStream,
    String filename,
    long sizeBytes
) {
}
