package com.example.keyworks.controller;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.service.SheetMusicService;
import com.example.keyworks.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final SheetMusicService sheetMusicService;
    private final FileService fileService;
    private final FileStorageConfig fileStorageConfig;

    public FileController(SheetMusicService sheetMusicService, FileService fileService, FileStorageConfig fileStorageConfig) {
        this.sheetMusicService = sheetMusicService;
        this.fileService = fileService;
        this.fileStorageConfig = fileStorageConfig;
        logger.info("FileController initialized with output directory: {}", fileStorageConfig.getOutputDirectory());
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        logger.info("Request to download PDF for sheet music ID: {}", id);
        
        Optional<SheetMusic> sheetMusicOpt = sheetMusicService.findSheetMusicById(id);
        
        if (sheetMusicOpt.isEmpty()) {
            logger.warn("Sheet music not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        SheetMusic sheetMusic = sheetMusicOpt.get();
        if (sheetMusic.getPdfPath() == null) {
            logger.warn("PDF path is null for sheet music ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        try {
            // Use the relative path stored in the database
            Resource resource = fileService.loadFileAsResource(sheetMusic.getPdfPath());
            logger.info("PDF resource loaded successfully for sheet music ID: {}", id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + sheetMusic.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to load PDF resource for sheet music ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/pdf/view/{id}")
    public ResponseEntity<Resource> viewPdf(@PathVariable Long id) {
        logger.info("Request to view PDF for sheet music ID: {}", id);
        
        Optional<SheetMusic> sheetMusicOpt = sheetMusicService.findSheetMusicById(id);
        
        if (sheetMusicOpt.isEmpty()) {
            logger.warn("Sheet music not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        SheetMusic sheetMusic = sheetMusicOpt.get();
        if (sheetMusic.getPdfPath() == null) {
            logger.warn("PDF path is null for sheet music ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        try {
            // Use the relative path stored in the database
            Resource resource = fileService.loadFileAsResource(sheetMusic.getPdfPath());
            logger.info("PDF resource loaded successfully for sheet music ID: {}", id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + sheetMusic.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to load PDF resource for sheet music ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/midi/{id}")
    public ResponseEntity<Resource> downloadMidi(@PathVariable Long id) {
        logger.info("Request to download MIDI for sheet music ID: {}", id);
        
        Optional<SheetMusic> sheetMusicOpt = sheetMusicService.findSheetMusicById(id);
        
        if (sheetMusicOpt.isEmpty()) {
            logger.warn("Sheet music not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        SheetMusic sheetMusic = sheetMusicOpt.get();
        if (sheetMusic.getMidiPath() == null) {
            logger.warn("MIDI path is null for sheet music ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        try {
            // Use the relative path stored in the database
            Resource resource = fileService.loadFileAsResource(sheetMusic.getMidiPath());
            logger.info("MIDI resource loaded successfully for sheet music ID: {}", id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/midi"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + sheetMusic.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".midi\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to load MIDI resource for sheet music ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/output/{filename:.+}")
    public ResponseEntity<Resource> serveOutputFile(@PathVariable String filename) {
        logger.info("Request to serve file from output directory: {}", filename);
        
        try {
            // Use the filename directly as the relative path
            Resource resource = fileService.loadFileAsResource(filename);
            
            String contentType;
            if (filename.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.endsWith(".midi") || filename.endsWith(".mid")) {
                contentType = "audio/midi";
            } else if (filename.endsWith(".ly")) {
                contentType = "text/plain";
            } else {
                contentType = "application/octet-stream";
            }
            
            logger.info("File {} loaded successfully with content type: {}", filename, contentType);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
        } catch (Exception e) {
            logger.error("Failed to serve file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}