package com.example.keyworks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
    

    public void init() {
        outputDirectory = Paths.get(outputDirectoryPath).toAbsolutePath().normalize();
        logger.info("Initialized file storage with output directory: {}", outputDirectory);
        
        try {
            Files.createDirectories(outputDirectory);
            logger.info("Output directory created/verified: {}", outputDirectory);
        } catch (IOException e) {
            logger.error("Could not create output directory: {}", outputDirectory, e);
            throw new RuntimeException("Could not create output directory", e);
        }
    }
    
    public Path getOutputDirectory() {
        return outputDirectory;
    }
    
    public Path resolveFilePath(String relativePath) {
        return outputDirectory.resolve(relativePath);
    }
    
    public String getRelativePath(Path absolutePath) {
        if (absolutePath.startsWith(outputDirectory)) {
            return outputDirectory.relativize(absolutePath).toString();
        }
        return absolutePath.getFileName().toString();
    }
}