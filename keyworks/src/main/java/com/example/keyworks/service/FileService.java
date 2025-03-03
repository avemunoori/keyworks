package com.example.keyworks.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {

    /**
     * Loads a file as a Resource
     * @param filePath The path to the file
     * @return The file as a Resource
     */
    public Resource loadFileAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error loading file: " + filePath, e);
        }
    }
}