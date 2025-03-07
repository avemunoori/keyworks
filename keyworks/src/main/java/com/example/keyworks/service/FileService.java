package com.example.keyworks.service;

import com.example.keyworks.config.FileStorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;

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
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                logger.debug("File exists and is readable: {}", filePath);
                return resource;
            } else {
                logger.error("File not found or not readable: {}", filePath);
                throw new RuntimeException("File not found or not readable: " + relativePath);
            }
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
            
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                logger.debug("File exists and is readable: {}", path);
                return resource;
            } else {
                logger.error("File not found or not readable: {}", path);
                throw new RuntimeException("File not found or not readable: " + absolutePath);
            }
        } catch (MalformedURLException e) {
            logger.error("Error loading file: {}", absolutePath, e);
            throw new RuntimeException("Error loading file: " + absolutePath, e);
        }
    }
}