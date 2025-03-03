package com.example.keyworks.service;

import com.example.keyworks.model.SheetMusic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LilyPondService {
    private static final Logger logger = LoggerFactory.getLogger(LilyPondService.class);

    @Value("${lilypond.executable:lilypond}")
    private String lilypondExecutable;

    @Value("${lilypond.output.dir:./output}")
    private String outputDirectory;

    /**
     * Generates PDF and MIDI files from LilyPond code
     * @param sheetMusic The SheetMusic entity containing LilyPond code
     * @return Updated SheetMusic with file paths
     */
    public SheetMusic generateFiles(SheetMusic sheetMusic) throws IOException, InterruptedException {
        // Create output directory if it doesn't exist
        Path outputDir = Paths.get(outputDirectory);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Generate a unique identifier for the files
        String fileId = UUID.randomUUID().toString();
        String baseFileName = fileId;
        String lilyFileName = baseFileName + ".ly";
        String pdfFileName = baseFileName + ".pdf";
        String midiFileName = baseFileName + ".midi";

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
        File pdfFile = outputDir.resolve(pdfFileName).toFile();
        File midiFile = outputDir.resolve(midiFileName).toFile();
        
        if (!pdfFile.exists()) {
            throw new RuntimeException("PDF file was not generated");
        }

        // Update the SheetMusic entity with file paths
        sheetMusic.setPdfPath(pdfFile.getAbsolutePath());
        if (midiFile.exists()) {
            sheetMusic.setMidiPath(midiFile.getAbsolutePath());
        }

        return sheetMusic;
    }

    /**
     * Deletes generated files for a SheetMusic entity
     * @param sheetMusic The SheetMusic entity
     */
    public void deleteFiles(SheetMusic sheetMusic) {
        if (sheetMusic.getPdfPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(sheetMusic.getPdfPath()));
            } catch (IOException e) {
                logger.error("Failed to delete PDF file: {}", sheetMusic.getPdfPath(), e);
            }
        }
        
        if (sheetMusic.getMidiPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(sheetMusic.getMidiPath()));
            } catch (IOException e) {
                logger.error("Failed to delete MIDI file: {}", sheetMusic.getMidiPath(), e);
            }
        }
    }
}