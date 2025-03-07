package com.example.keyworks.service;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.model.SheetMusic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LilyPondService {
    private static final Logger logger = LoggerFactory.getLogger(LilyPondService.class);

    @Value("${lilypond.executable:lilypond}")
    private String lilypondExecutable;

    private final FileStorageConfig fileStorageConfig;

    public LilyPondService(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
    }

    /**
     * Generates PDF and MIDI files from LilyPond code
     * @param sheetMusic The SheetMusic entity containing LilyPond code
     * @return Updated SheetMusic with file paths
     */
    public SheetMusic generateFiles(SheetMusic sheetMusic) throws IOException, InterruptedException {
        // Generate a unique identifier for the files
        String fileId = UUID.randomUUID().toString();
        String baseFileName = fileId;
        String lilyFileName = baseFileName + ".ly";
        String pdfFileName = baseFileName + ".pdf";
        String midiFileName = baseFileName + ".midi";

        // Get the output directory
        Path outputDir = fileStorageConfig.getOutputDirectory();
        
        // Create the LilyPond file
        Path lilyFilePath = outputDir.resolve(lilyFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(lilyFilePath.toFile()))) {
            writer.write(sheetMusic.getLilyPondCode());
        }

        // Execute LilyPond to generate PDF and MIDI
        ProcessBuilder processBuilder = new ProcessBuilder(
                lilypondExecutable,
                "--pdf",
                "--output=" + outputDir.toAbsolutePath(),
                lilyFilePath.toAbsolutePath().toString()
        );
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("LilyPond process timed out");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("LilyPond process failed with exit code: " + exitCode);
        }

        // Check if files were generated
        Path pdfFilePath = outputDir.resolve(pdfFileName);
        Path midiFilePath = outputDir.resolve(midiFileName);
        
        if (!Files.exists(pdfFilePath)) {
            throw new RuntimeException("PDF file was not generated");
        }

        // Store relative paths instead of absolute paths
        sheetMusic.setPdfPath(pdfFileName);
        
        if (Files.exists(midiFilePath)) {
            sheetMusic.setMidiPath(midiFileName);
        }

        logger.info("Generated files for sheet music '{}': PDF={}, MIDI={}", 
                sheetMusic.getTitle(), pdfFileName, 
                Files.exists(midiFilePath) ? midiFileName : "not generated");

        return sheetMusic;
    }

    /**
     * Deletes generated files for a SheetMusic entity
     * @param sheetMusic The SheetMusic entity
     */
    public void deleteFiles(SheetMusic sheetMusic) {
        if (sheetMusic.getPdfPath() != null) {
            try {
                Path pdfPath = fileStorageConfig.resolveFilePath(sheetMusic.getPdfPath());
                Files.deleteIfExists(pdfPath);
                logger.info("Deleted PDF file: {}", pdfPath);
            } catch (IOException e) {
                logger.error("Failed to delete PDF file: {}", sheetMusic.getPdfPath(), e);
            }
        }
        
        if (sheetMusic.getMidiPath() != null) {
            try {
                Path midiPath = fileStorageConfig.resolveFilePath(sheetMusic.getMidiPath());
                Files.deleteIfExists(midiPath);
                logger.info("Deleted MIDI file: {}", midiPath);
            } catch (IOException e) {
                logger.error("Failed to delete MIDI file: {}", sheetMusic.getMidiPath(), e);
            }
        }
    }
}