package com.example.keyworks.service;

import com.example.keyworks.config.FileStorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    
    private final FileStorageConfig fileStorageConfig;
    
    public FileService(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
    }

    /**
     * Loads a file as a Resource using a relative path
     * @param relativePath The relative path to the file
     * @return The file as a Resource
     */
    public Resource loadFileAsResource(String relativePath) {
        try {
            Path filePath = fileStorageConfig.resolveFilePath(relativePath);
            logger.debug("Resolving file: {} to absolute path: {}", relativePath, filePath);
            
            // Check if file exists
            File file = filePath.toFile();
            if (!file.exists()) {
                logger.error("File does not exist: {}", filePath);
                throw new RuntimeException("File not found: " + relativePath);
            }
            
            if (!file.canRead()) {
                logger.error("File is not readable: {}", filePath);
                throw new RuntimeException("File not readable: " + relativePath);
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            logger.info("Successfully loaded resource: {}", filePath);
            
            return resource;
        } catch (MalformedURLException e) {
            logger.error("Error loading file: {}", relativePath, e);
            throw new RuntimeException("Error loading file: " + relativePath, e);
        }
    }
    
    /**
     * Loads a file as a Resource using an absolute path
     * This method is provided for backward compatibility
     * @param absolutePath The absolute path to the file
     * @return The file as a Resource
     */
    public Resource loadFileAsResourceFromAbsolutePath(String absolutePath) {
        try {
            Path path = Path.of(absolutePath);
            logger.debug("Loading file from absolute path: {}", path);
            
            // Check if file exists
            File file = path.toFile();
            if (!file.exists()) {
                logger.error("File does not exist: {}", path);
                throw new RuntimeException("File not found: " + absolutePath);
            }
            
            if (!file.canRead()) {
                logger.error("File is not readable: {}", path);
                throw new RuntimeException("File not readable: " + absolutePath);
            }
            
            Resource resource = new UrlResource(path.toUri());
            logger.info("Successfully loaded resource from absolute path: {}", path);
            
            return resource;
        } catch (MalformedURLException e) {
            logger.error("Error loading file: {}", absolutePath, e);
            throw new RuntimeException("Error loading file: " + absolutePath, e);
        }
    }
}