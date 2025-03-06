package com.example.keyworks.controller;

import com.example.keyworks.service.MidiProcessingService;
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

    private final MidiProcessingService midiProcessingService;


    public TestController(MidiProcessingService midiProcessingService) {
        this.midiProcessingService = midiProcessingService;
    }

    @GetMapping("/generate-pdf/{id}")
    public ResponseEntity<String> generateTestPdf(@PathVariable String id) {
        // Generate a test PDF with some notes
        List<String> testNotes = Arrays.asList("c4", "d4", "e4", "f4", "g4", "a4", "b4", "c'4");
        boolean success = midiProcessingService.processMidiToLilyPond(id, testNotes);
        
        if (success) {
            String pdfPath = midiProcessingService.getPdfPath(id);
            String outputDir = midiProcessingService.getOutputDirectory();
            
            return ResponseEntity.ok(
                "PDF generated successfully!\n" +
                "PDF Path: " + pdfPath + "\n" +
                "Output Directory: " + outputDir + "\n" +
                "Access URL: /output/" + id + ".pdf"
            );
        } else {
            return ResponseEntity.internalServerError().body("Failed to generate PDF");
        }
    }
    
    @GetMapping("/check-file/{id}")
    public ResponseEntity<String> checkFile(@PathVariable String id) {
        String outputDir = midiProcessingService.getOutputDirectory();
        Path pdfPath = Paths.get(outputDir, id + ".pdf");
        
        StringBuilder result = new StringBuilder();
        result.append("Checking file: ").append(pdfPath).append("\n");
        result.append("File exists: ").append(Files.exists(pdfPath)).append("\n");
        
        if (Files.exists(pdfPath)) {
            try {
                result.append("File size: ").append(Files.size(pdfPath)).append(" bytes\n");
                result.append("File is readable: ").append(Files.isReadable(pdfPath)).append("\n");
                result.append("Access URL: /output/").append(id).append(".pdf");
            } catch (IOException e) {
                result.append("Error checking file: ").append(e.getMessage());
            }
        }
        
        return ResponseEntity.ok(result.toString());
    }
    
    @GetMapping("/list-files")
    public ResponseEntity<String> listFiles() {
        String outputDir = midiProcessingService.getOutputDirectory();
        Path dirPath = Paths.get(outputDir);
        
        StringBuilder result = new StringBuilder();
        result.append("Output directory: ").append(outputDir).append("\n");
        result.append("Directory exists: ").append(Files.exists(dirPath)).append("\n");
        
        if (Files.exists(dirPath)) {
            try {
                result.append("Files in directory:\n");
                Files.list(dirPath).forEach(path -> {
                    try {
                        result.append("- ").append(path.getFileName())
                              .append(" (").append(Files.size(path)).append(" bytes)\n");
                    } catch (IOException e) {
                        result.append("- ").append(path.getFileName())
                              .append(" (error getting size: ").append(e.getMessage()).append(")\n");
                    }
                });
            } catch (IOException e) {
                result.append("Error listing files: ").append(e.getMessage());
            }
        }
        
        return ResponseEntity.ok(result.toString());
    }
}