package ru.storage.filestorageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main entry point for the File Storage Service application.
 * <p>
 * This is a Spring Boot application that provides file upload, download,
 * validation, and asynchronous deletion capabilities backed by PostgreSQL
 * and MinIO. Retry support is enabled for transient failures.
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
public class FileStorageServiceApplication {

    /**
     * Launches the Spring Boot application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(FileStorageServiceApplication.class, args);
    }
}
