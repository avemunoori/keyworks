package com.example.keyworks.controller;

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
        // Generate a test PDF with some notes
        List<String> testNotes = Arrays.asList("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5");
        boolean success = midiProcessingService.processMidiToLilyPond(id, testNotes);
        
        if (success) {
            Path pdfPath = midiProcessingService.getPdfPath(id);
            Path outputDir = midiProcessingService.getOutputDirectory();
            
            return ResponseEntity.ok(
                "PDF generated successfully!\n" +
                "PDF Path: " + pdfPath.toAbsolutePath() + "\n" +
                "Output Directory: " + outputDir.toAbsolutePath() + "\n" +
                "Access URL: /api/files/output/" + id + ".pdf"
            );
        } else {
            return ResponseEntity.internalServerError().body("Failed to generate PDF");
        }
    }
    
    @GetMapping("/check-file/{id}")
    public ResponseEntity<String> checkFile(@PathVariable String id) {
        Path outputDir = midiProcessingService.getOutputDirectory();
        Path pdfPath = outputDir.resolve(id + ".pdf");
        
        StringBuilder result = new StringBuilder();
        result.append("Checking file: ").append(pdfPath.toAbsolutePath()).append("\n");
        result.append("File exists: ").append(Files.exists(pdfPath)).append("\n");
        
        if (Files.exists(pdfPath)) {
            try {
                result.append("File size: ").append(Files.size(pdfPath)).append(" bytes\n");
                result.append("File is readable: ").append(Files.isReadable(pdfPath)).append("\n");
                result.append("Access URL: /api/files/output/").append(id).append(".pdf");
            } catch (IOException e) {
                logger.error("Error checking file: {}", e.getMessage(), e);
                result.append("Error checking file: ").append(e.getMessage());
            }
        }
        
        return ResponseEntity.ok(result.toString());
    }
    
    @GetMapping("/list-files")
    public ResponseEntity<String> listFiles() {
        Path outputDir = midiProcessingService.getOutputDirectory();
        
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