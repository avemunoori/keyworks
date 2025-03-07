package com.example.keyworks.service;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.model.SheetMusic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LilyPondService {
    private static final Logger logger = LoggerFactory.getLogger(LilyPondService.class);
    
    private final FileStorageConfig fileStorageConfig;
    
    public LilyPondService(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
        logger.info("LilyPondService initialized with output directory: {}", fileStorageConfig.getOutputDirectory());
    }
    
    /**
     * Generates PDF and MIDI files from a SheetMusic entity
     * @param sheetMusic The SheetMusic entity containing LilyPond code
     * @return The updated SheetMusic entity with file paths
     */
    public SheetMusic generateFiles(SheetMusic sheetMusic) throws InterruptedException, IOException {
        String lilyPondCode = sheetMusic.getLilyPondCode();
        
        // Convert Long ID to String
        Long id = sheetMusic.getId();
        String fileId = id != null ? id.toString() : UUID.randomUUID().toString();
        
        Map<String, String> generatedFiles = generateFilesInternal(lilyPondCode, fileId);
        
        // Update the SheetMusic object with the generated file paths
        if (generatedFiles.containsKey("pdf")) {
            sheetMusic.setPdfPath(generatedFiles.get("pdf"));
        }
        
        if (generatedFiles.containsKey("midi")) {
            sheetMusic.setMidiPath(generatedFiles.get("midi"));
        }
        
        return sheetMusic;
    }
    
    /**
     * Internal method to generate PDF and MIDI files from LilyPond code
     * @param lilyPondCode The LilyPond code to generate files from
     * @param fileId The ID to use for the generated files
     * @return A map containing the paths to the generated files
     */
    private Map<String, String> generateFilesInternal(String lilyPondCode, String fileId) throws InterruptedException, IOException {
        // Generate a unique identifier for the files
        String uniqueId = fileId != null ? fileId : UUID.randomUUID().toString();
        String filePrefix = "music_generated_" + uniqueId;
        String lilyFileName = filePrefix + ".ly";
        String pdfFileName = filePrefix + ".pdf";
        String midiFileName = filePrefix + ".midi";
        
        // Get output directory
        Path outputDir = fileStorageConfig.getOutputDirectoryPath();
        File outputDirFile = outputDir.toFile();
        
        // Ensure output directory exists
        if (!outputDirFile.exists()) {
            logger.info("Creating output directory: {}", outputDir);
            Files.createDirectories(outputDir);
        }
        
        // Create the LilyPond file path
        Path lilyFilePath = outputDir.resolve(lilyFileName);
        
        logger.info("Creating LilyPond file at: {}", lilyFilePath);
        try (BufferedWriter writer = Files.newBufferedWriter(lilyFilePath)) {
            writer.write(lilyPondCode);
        }
        
        // Execute LilyPond to generate PDF and MIDI
        ProcessBuilder processBuilder = new ProcessBuilder(
            "lilypond",
            "--pdf",
            "--output=" + outputDir.toString(),
            lilyFilePath.getFileName().toString()
        );
        
        // Set working directory to output directory
        processBuilder.directory(outputDirFile);
        
        // Redirect error stream to standard output for better logging
        processBuilder.redirectErrorStream(true);
        
        logger.info("Executing LilyPond command in directory: {}", outputDir);
        logger.debug("Command: lilypond --pdf --output={} {}", 
                outputDir.toString(), lilyFilePath.getFileName().toString());
        
        Process process = processBuilder.start();
        
        // Wait for the process to complete with timeout
        boolean completed = process.waitFor(30L, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("LilyPond process timed out");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("LilyPond process failed with exit code: " + exitCode);
        }
        
        // Check generated files
        Path pdfFilePath = outputDir.resolve(pdfFileName);
        Path midiFilePath = outputDir.resolve(midiFileName);
        
        // Log the directory contents for debugging
        logger.debug("Output directory contents after LilyPond execution:");
        File[] files = outputDirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                logger.debug(" - {}: {} bytes", file.getName(), file.length());
            }
        }
        
        Map<String, String> generatedFiles = new HashMap<>();
        
        if (!Files.exists(pdfFilePath)) {
            logger.error("PDF file was not generated at expected path: {}", pdfFilePath);
            
            // Try to find the PDF file by listing the directory
            File[] pdfFiles = outputDirFile.listFiles((dir, name) -> name.endsWith(".pdf") && name.contains(uniqueId));
            if (pdfFiles != null && pdfFiles.length > 0) {
                pdfFilePath = pdfFiles[0].toPath();
                pdfFileName = pdfFiles[0].getName();
                logger.info("Found PDF file at alternative location: {}", pdfFilePath);
            } else {
                throw new RuntimeException("PDF file was not generated");
            }
        }
        
        // Add PDF file to the map
        generatedFiles.put("pdf", pdfFileName);
        logger.info("Generated PDF file: {}", pdfFileName);
        
        if (Files.exists(midiFilePath)) {
            generatedFiles.put("midi", midiFileName);
            logger.info("Generated MIDI file: {}", midiFileName);
        } else {
            logger.warn("MIDI file was not generated at expected path: {}", midiFilePath);
            
            // Try to find the MIDI file by listing the directory
            File[] midiFiles = outputDirFile.listFiles((dir, name) -> 
                (name.endsWith(".midi") || name.endsWith(".mid")) && name.contains(uniqueId));
            if (midiFiles != null && midiFiles.length > 0) {
                midiFileName = midiFiles[0].getName();
                generatedFiles.put("midi", midiFileName);
                logger.info("Found MIDI file at alternative location: {}", midiFiles[0].getPath());
            } else {
                logger.warn("No MIDI file was generated");
            }
        }
        
        return generatedFiles;
    }
    
    /**
     * Generates PDF and MIDI files from LilyPond code
     * @param lilyPondCode The LilyPond code to generate files from
     * @param fileId The ID to use for the generated files
     * @return A map containing the paths to the generated files
     */
    public Map<String, String> generateFiles(String lilyPondCode, String fileId) throws InterruptedException, IOException {
        return generateFilesInternal(lilyPondCode, fileId);
    }
    
    /**
     * Gets the path to the PDF file for a given sheet music ID
     * @param fileId The ID of the sheet music
     * @return The path to the PDF file
     */
    public String getPdfFilePath(String fileId) {
        String pdfFileName = "music_generated_" + fileId + ".pdf";
        Path pdfFilePath = fileStorageConfig.resolveFilePath(pdfFileName);
        
        if (Files.exists(pdfFilePath)) {
            return pdfFileName;
        }
        
        // If the file doesn't exist at the expected path, try to find it
        logger.warn("PDF file not found at expected path: {}", pdfFilePath);
        
        File outputDirFile = fileStorageConfig.getOutputDirectoryPath().toFile();
        File[] pdfFiles = outputDirFile.listFiles((dir, name) -> name.endsWith(".pdf") && name.contains(fileId));
        
        if (pdfFiles != null && pdfFiles.length > 0) {
            logger.info("Found PDF file at alternative location: {}", pdfFiles[0].getPath());
            return pdfFiles[0].getName();
        }
        
        logger.error("PDF file not found for ID: {}", fileId);
        throw new RuntimeException("PDF file not found for ID: " + fileId);
    }
    
    /**
     * Gets the path to the MIDI file for a given sheet music ID
     * @param fileId The ID of the sheet music
     * @return The path to the MIDI file
     */
    public String getMidiFilePath(String fileId) {
        String midiFileName = "music_generated_" + fileId + ".midi";
        Path midiFilePath = fileStorageConfig.resolveFilePath(midiFileName);
        
        if (Files.exists(midiFilePath)) {
            return midiFileName;
        }
        
        // If the file doesn't exist at the expected path, try to find it
        logger.warn("MIDI file not found at expected path: {}", midiFilePath);
        
        File outputDirFile = fileStorageConfig.getOutputDirectoryPath().toFile();
        File[] midiFiles = outputDirFile.listFiles((dir, name) -> 
            (name.endsWith(".midi") || name.endsWith(".mid")) && name.contains(fileId));
        
        if (midiFiles != null && midiFiles.length > 0) {
            logger.info("Found MIDI file at alternative location: {}", midiFiles[0].getPath());
            return midiFiles[0].getName();
        }
        
        logger.error("MIDI file not found for ID: {}", fileId);
        throw new RuntimeException("MIDI file not found for ID: " + fileId);
    }
    
    /**
     * Deletes the files associated with a sheet music ID
     * @param fileId The ID of the sheet music
     */
    public void deleteFiles(String fileId) {
        try {
            Path outputDir = fileStorageConfig.getOutputDirectoryPath();
            File outputDirFile = outputDir.toFile();
            
            // Find and delete all files with the given ID
            File[] files = outputDirFile.listFiles((dir, name) -> name.contains(fileId));
            if (files != null) {
                for (File file : files) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        logger.info("Deleted file: {}", file.getPath());
                    } else {
                        logger.warn("Failed to delete file: {}", file.getPath());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting files for ID {}: {}", fileId, e.getMessage(), e);
        }
    }
    
    /**
     * Deletes the files associated with a sheet music entity
     * @param sheetMusic The sheet music entity
     */
    public void deleteFiles(SheetMusic sheetMusic) {
        Long id = sheetMusic.getId();
        if (id != null) {
            deleteFiles(id.toString());
        }
    }
}