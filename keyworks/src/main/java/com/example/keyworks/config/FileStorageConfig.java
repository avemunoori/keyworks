package com.example.keyworks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfig.class);
    
    private final String outputDirectory;
    private Path outputDirectoryPath;
    
    public FileStorageConfig(@Value("${app.output.directory:./output}") String outputDirectory) {
        this.outputDirectory = outputDirectory;
        // Initialize the path immediately in the constructor
        this.outputDirectoryPath = Paths.get(outputDirectory).toAbsolutePath().normalize();
        logger.info("FileStorageConfig initialized with output directory: {}", this.outputDirectoryPath);
    }

    public void init() {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(outputDirectoryPath);
            logger.info("Output directory created/verified: {}", outputDirectoryPath);
        } catch (IOException e) {
            logger.error("Could not create output directory: {}", e.getMessage());
            throw new RuntimeException("Could not create output directory", e);
        }
    }
    
    public String getOutputDirectory() {
        return outputDirectoryPath.toString();
    }
    
    public Path getOutputDirectoryPath() {
        return outputDirectoryPath;
    }
    
    /**
     * Resolves a relative file path against the output directory
     * @param relativePath The relative path to resolve
     * @return The absolute path
     */
    public Path resolveFilePath(String relativePath) {
        // Remove any leading slashes to ensure it's treated as relative
        String cleanPath = StringUtils.cleanPath(relativePath);
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        
        Path resolvedPath = outputDirectoryPath.resolve(cleanPath).normalize();
        
        // Security check to prevent directory traversal attacks
        if (!resolvedPath.startsWith(outputDirectoryPath)) {
            throw new RuntimeException("Security violation: Attempted to access file outside of output directory: " + relativePath);
        }
        
        logger.debug("Resolved path {} to {}", relativePath, resolvedPath);
        return resolvedPath;
    }
    
    /**
     * Converts an absolute path to a relative path (relative to the output directory)
     * @param absolutePath The absolute path to convert
     * @return The relative path
     */
    public String getRelativePath(String absolutePath) {
        Path path = Paths.get(absolutePath).normalize();
        
        // Check if the path is within the output directory
        if (path.startsWith(outputDirectoryPath)) {
            return outputDirectoryPath.relativize(path).toString();
        }
        
        // If not within the output directory, return the original path
        // This is for backward compatibility
        logger.warn("Path is not within output directory: {}", absolutePath);
        return absolutePath;
    }
}