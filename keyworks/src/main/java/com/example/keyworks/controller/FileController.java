package com.example.keyworks.controller;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/files")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    private final FileService fileService;
    private final FileStorageConfig fileStorageConfig;
    
    public FileController(FileService fileService, FileStorageConfig fileStorageConfig) {
        this.fileService = fileService;
        this.fileStorageConfig = fileStorageConfig;
        logger.info("FileController initialized with output directory: {}", fileStorageConfig.getOutputDirectory());
    }
    
    /**
     * Serves a file from the output directory
     * @param filename The name of the file to serve
     * @return The file as a Resource
     */
    @GetMapping("/output/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveOutputFile(@PathVariable String filename) {
        logger.info("Request to serve file from output directory: {}", filename);
        
        try {
            // First, check if the file exists directly in the output directory
            Path filePath = fileStorageConfig.resolveFilePath(filename);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                logger.warn("File not found at expected path: {}", filePath);
                
                // Try to find the file by listing the output directory
                String outputDirStr = fileStorageConfig.getOutputDirectory();
                File outputDir = new File(outputDirStr);
                File[] files = outputDir.listFiles((dir, name) -> name.equals(filename));
                
                if (files != null && files.length > 0) {
                    file = files[0];
                    logger.info("Found file by directory listing: {}", file.getAbsolutePath());
                } else {
                    // Try with ./ prefix removed (sometimes LilyPond adds this)
                    if (filename.startsWith("./")) {
                        String altFilename = filename.substring(2);
                        files = outputDir.listFiles((dir, name) -> name.equals(altFilename));
                        
                        if (files != null && files.length > 0) {
                            file = files[0];
                            logger.info("Found file with ./ prefix removed: {}", file.getAbsolutePath());
                        } else {
                            logger.error("Failed to serve file {}: File not found or not readable: {}", filename, filename);
                            return ResponseEntity.notFound().build();
                        }
                    } else {
                        logger.error("Failed to serve file {}: File not found or not readable: {}", filename, filename);
                        return ResponseEntity.notFound().build();
                    }
                }
            }
            
            // Load the file as a resource
            Resource resource;
            if (file.isAbsolute()) {
                resource = fileService.loadFileAsResourceFromAbsolutePath(file.getAbsolutePath());
            } else {
                resource = fileService.loadFileAsResource(filename);
            }
            
            // Determine content type
            String contentType;
            try {
                contentType = Files.probeContentType(file.toPath());
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }
            
            if (contentType == null) {
                // Default content types based on extension
                if (filename.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (filename.endsWith(".mid") || filename.endsWith(".midi")) {
                    contentType = "audio/midi";
                } else if (filename.endsWith(".ly")) {
                    contentType = "text/plain";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            logger.info("Serving file {} with content type: {}", filename, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Failed to serve file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Lists all files in the output directory
     * @return A JSON response with information about the output directory and its contents
     */
    @GetMapping("/debug/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listOutputDirectory() {
        logger.info("Listing output directory contents");
        
        Map<String, Object> response = new HashMap<>();
        response.put("outputDirectory", fileStorageConfig.getOutputDirectory());
        
        try {
            String outputDirStr = fileStorageConfig.getOutputDirectory();
            File dir = new File(outputDirStr);
            File[] files = dir.listFiles();
            
            List<Map<String, String>> fileList = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("size", String.valueOf(file.length()));
                    fileInfo.put("isDirectory", String.valueOf(file.isDirectory()));
                    fileInfo.put("canRead", String.valueOf(file.canRead()));
                    fileInfo.put("lastModified", String.valueOf(file.lastModified()));
                    fileList.add(fileInfo);
                }
            }
            
            response.put("files", fileList);
            response.put("count", fileList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error listing output directory: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Gets information about a specific file in the output directory
     * @param filename The name of the file to get information about
     * @return A JSON response with information about the file
     */
    @GetMapping("/debug/info/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String filename) {
        logger.info("Getting info for file: {}", filename);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = fileStorageConfig.resolveFilePath(filename);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                logger.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            response.put("name", file.getName());
            response.put("path", file.getAbsolutePath());
            response.put("size", file.length());
            response.put("isDirectory", file.isDirectory());
            response.put("canRead", file.canRead());
            response.put("lastModified", file.lastModified());
            
            // Try to determine content type
            try {
                String contentType = Files.probeContentType(file.toPath());
                response.put("contentType", contentType);
            } catch (IOException e) {
                response.put("contentType", "unknown");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting file info: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Serves a file directly from the file system (for backward compatibility)
     * @param path The path to the file to serve
     * @return The file as a Resource
     */
    @GetMapping("/direct")
    @ResponseBody
    public ResponseEntity<Resource> serveFileDirectly(String path) {
        logger.info("Request to serve file directly: {}", path);
        
        try {
            // Security check to prevent directory traversal
            File file = new File(path);
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                logger.error("File not found or not readable: {}", path);
                return ResponseEntity.notFound().build();
            }
            
            // Load the file as a resource
            Resource resource = fileService.loadFileAsResourceFromAbsolutePath(path);
            
            // Determine content type
            String contentType;
            try {
                contentType = Files.probeContentType(file.toPath());
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }
            
            if (contentType == null) {
                // Default content types based on extension
                String filename = file.getName().toLowerCase();
                if (filename.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (filename.endsWith(".mid") || filename.endsWith(".midi")) {
                    contentType = "audio/midi";
                } else if (filename.endsWith(".ly")) {
                    contentType = "text/plain";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            logger.info("Serving file {} with content type: {}", path, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Failed to serve file {}: {}", path, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}