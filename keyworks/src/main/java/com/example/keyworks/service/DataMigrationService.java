package com.example.keyworks.service;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.repository.SheetMusicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DataMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);
    
    private final SheetMusicRepository sheetMusicRepository;
    private final FileStorageConfig fileStorageConfig;
    
    public DataMigrationService(SheetMusicRepository sheetMusicRepository, FileStorageConfig fileStorageConfig) {
        this.sheetMusicRepository = sheetMusicRepository;
        this.fileStorageConfig = fileStorageConfig;
    }
    
    /**
     * Converts absolute file paths to relative paths in the database
     * This method runs automatically when the application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateFilePaths() {
        logger.info("Starting file path migration...");
        
        List<SheetMusic> allSheetMusic = sheetMusicRepository.findAll();
        int updatedCount = 0;
        
        for (SheetMusic sheetMusic : allSheetMusic) {
            boolean updated = false;
            
            // Convert PDF path if it exists and is absolute
            if (sheetMusic.getPdfPath() != null && Paths.get(sheetMusic.getPdfPath()).isAbsolute()) {
                Path absolutePdfPath = Paths.get(sheetMusic.getPdfPath());
                String filename = absolutePdfPath.getFileName().toString();
                sheetMusic.setPdfPath(filename);
                updated = true;
                logger.debug("Converted PDF path for sheet music ID {}: {} -> {}", 
                        sheetMusic.getId(), absolutePdfPath, filename);
            }
            
            // Convert MIDI path if it exists and is absolute
            if (sheetMusic.getMidiPath() != null && Paths.get(sheetMusic.getMidiPath()).isAbsolute()) {
                Path absoluteMidiPath = Paths.get(sheetMusic.getMidiPath());
                String filename = absoluteMidiPath.getFileName().toString();
                sheetMusic.setMidiPath(filename);
                updated = true;
                logger.debug("Converted MIDI path for sheet music ID {}: {} -> {}", 
                        sheetMusic.getId(), absoluteMidiPath, filename);
            }
            
            if (updated) {
                sheetMusicRepository.save(sheetMusic);
                updatedCount++;
            }
        }
        
        logger.info("File path migration completed. Updated {} sheet music records.", updatedCount);
    }
}