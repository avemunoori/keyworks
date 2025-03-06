package com.example.keyworks.controller;

import com.example.keyworks.model.Note;
import com.example.keyworks.service.MidiProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    private final MidiProcessingService midiProcessingService;

    public TestController(MidiProcessingService midiProcessingService) {
        this.midiProcessingService = midiProcessingService;
    }

    @GetMapping("/generate-pdf/{id}")
    public ResponseEntity<String> generateTestPdf(@PathVariable String id) {
        // Clear any existing notes
        midiProcessingService.clearNotes();
        
        // Add test notes (simulating MIDI input)
        long timestamp = System.currentTimeMillis();
        addTestNotes(timestamp);
        
        // Generate LilyPond file and compile to PDF
        String baseFilename = midiProcessingService.generateLilyPondFile();
        
        if (baseFilename != null) {
            Path outputDir = Paths.get(midiProcessingService.getOutputDirectory());
            Path pdfPath = outputDir.resolve(baseFilename + ".pdf");
            
            return ResponseEntity.ok(
                "PDF generated successfully!\n" +
                "PDF Path: " + pdfPath.toAbsolutePath() + "\n" +
                "Output Directory: " + outputDir.toAbsolutePath() + "\n" +
                "Access URL: /api/files/output/" + baseFilename + ".pdf"
            );
        } else {
            return ResponseEntity.internalServerError().body("Failed to generate PDF");
        }
    }
    
    private void addTestNotes(long startTimestamp) {
        // Define test notes (C4 to C5)
        int[] noteNumbers = {60, 62, 64, 65, 67, 69, 71, 72}; // C4, D4, E4, F4, G4, A4, B4, C5
        int velocity = 64; // Medium velocity
        long duration = 500; // 500ms duration for each note
        
        // Add notes with increasing timestamps
        for (int i = 0; i < noteNumbers.length; i++) {
            long noteTimestamp = startTimestamp + (i * 600); // 600ms between notes
            midiProcessingService.processNoteOn(noteNumbers[i], velocity, noteTimestamp);
            midiProcessingService.processNoteOff(noteNumbers[i], noteTimestamp + duration);
        }
    }
    
    @GetMapping("/check-file/{filename}")
    public ResponseEntity<String> checkFile(@PathVariable String filename) {
        Path outputDir = Paths.get(midiProcessingService.getOutputDirectory());
        Path pdfPath = outputDir.resolve(filename + ".pdf");
        
        StringBuilder result = new StringBuilder();
        result.append("Checking file: ").append(pdfPath.toAbsolutePath()).append("\n");
        result.append("File exists: ").append(Files.exists(pdfPath)).append("\n");
        
        if (Files.exists(pdfPath)) {
            try {
                result.append("File size: ").append(Files.size(pdfPath)).append(" bytes\n");
                result.append("File is readable: ").append(Files.isReadable(pdfPath)).append("\n");
                result.append("Access URL: /api/files/output/").append(filename).append(".pdf");
            } catch (IOException e) {
                logger.error("Error checking file: {}", e.getMessage(), e);
                result.append("Error checking file: ").append(e.getMessage());
            }
        }
        
        return ResponseEntity.ok(result.toString());
    }
    
    @GetMapping("/list-files")
    public ResponseEntity<String> listFiles() {
        Path outputDir = Paths.get(midiProcessingService.getOutputDirectory());
        
        StringBuilder result = new StringBuilder();
        result.append("Output directory: ").append(outputDir.toAbsolutePath()).append("\n");
        result.append("Directory exists: ").append(Files.exists(outputDir)).append("\n");
        
        if (Files.exists(outputDir)) {
            try {
                result.append("Files in directory:\n");
                Files.list(outputDir).forEach(path -> {
                    try {
                        result.append("- ").append(path.getFileName())
                              .append(" (").append(Files.size(path)).append(" bytes)\n");
                    } catch (IOException e) {
                        logger.error("Error getting file size for {}: {}", path, e.getMessage(), e);
                        result.append("- ").append(path.getFileName())
                              .append(" (error getting size: ").append(e.getMessage()).append(")\n");
                    }
                });
            } catch (IOException e) {
                logger.error("Error listing files: {}", e.getMessage(), e);
                result.append("Error listing files: ").append(e.getMessage());
            }
        }
        
        return ResponseEntity.ok(result.toString());
    }
}