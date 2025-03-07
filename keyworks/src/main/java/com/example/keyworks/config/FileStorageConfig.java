package com.example.keyworks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileStorageConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfig.class);
    
    @Value("${lilypond.output.dir:./output}")
    private String outputDirectoryPath;
    
    private Path outputDirectory;
    
    @PostConstruct
    public void init() {
        try {
            outputDirectory = Paths.get(outputDirectoryPath).toAbsolutePath().normalize();
            logger.info("Initialized file storage with output directory: {}", outputDirectory);
            
            Files.createDirectories(outputDirectory);
            logger.info("Output directory created/verified: {}", outputDirectory);
        } catch (IOException e) {
            logger.error("Could not create output directory: {}", outputDirectoryPath, e);
            throw new RuntimeException("Could not create output directory", e);
        }
    }
    
    public Path getOutputDirectory() {
        return outputDirectory;
    }
    
    public Path resolveFilePath(String relativePath) {
        if (outputDirectory == null) {
            throw new IllegalStateException("Output directory not initialized");
        }
        return outputDirectory.resolve(relativePath);
    }
    
    public String getRelativePath(Path absolutePath) {
        if (outputDirectory == null) {
            throw new IllegalStateException("Output directory not initialized");
        }
        if (absolutePath.startsWith(outputDirectory)) {
            return outputDirectory.relativize(absolutePath).toString();
        }
        return absolutePath.getFileName().toString();
    }
}