package com.example.keyworks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class MidiProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MidiProcessingService.class);
    private final Path outputDir;
    
    public MidiProcessingService() {
        // Get the output directory path - use absolute path
        String outputDirPath = System.getProperty("user.dir") + File.separator + "output";
        this.outputDir = Paths.get(outputDirPath).toAbsolutePath();
        
        // Create output directory if it doesn't exist
        try {
            Files.createDirectories(outputDir);
            logger.info("MidiProcessingService initialized with output directory: {}", outputDir.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process MIDI notes and generate LilyPond notation and PDF
     * @param recordingId The unique ID for this recording
     * @param notes The list of MIDI notes
     * @return true if processing was successful
     */
    public boolean processMidiToLilyPond(String recordingId, List<String> notes) {
        try {
            logger.info("Processing MIDI to LilyPond for recording ID: {}", recordingId);
            
            // Generate LilyPond file
            String lilypondContent = generateLilyPondNotation(notes);
            Path lilypondPath = outputDir.resolve(recordingId + ".ly");
            Files.write(lilypondPath, lilypondContent.getBytes());
            
            logger.info("LilyPond file created at: {}", lilypondPath.toAbsolutePath());
            logger.info("LilyPond file exists: {}", Files.exists(lilypondPath));
            
            // Compile LilyPond to PDF (this would call the LilyPond executable)
            boolean pdfGenerated = compileLilyPondToPdf(lilypondPath.toString());
            
            // Generate MIDI file
            generateMidiFile(recordingId, notes);
            
            // Verify all files after generation
            Path pdfPath = outputDir.resolve(recordingId + ".pdf");
            Path midiPath = outputDir.resolve(recordingId + ".midi");
            
            logger.info("PDF file path: {}", pdfPath.toAbsolutePath());
            logger.info("PDF file exists: {}", Files.exists(pdfPath));
            logger.info("PDF file URL: /output/{}.pdf", recordingId);
            logger.info("MIDI file exists: {}", Files.exists(midiPath));
            
            return pdfGenerated;
        } catch (IOException e) {
            logger.error("Error processing MIDI to LilyPond: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate LilyPond notation from MIDI notes
     * @param notes The list of MIDI notes
     * @return LilyPond notation as a string
     */
    private String generateLilyPondNotation(List<String> notes) {
        // This is a simplified example - in a real app, you'd have more complex logic
        StringBuilder sb = new StringBuilder();
        sb.append("\\version \"2.20.0\"\n");
        sb.append("\\score {\n");
        sb.append("  \\new Staff {\n");
        sb.append("    \\clef treble\n");
        sb.append("    \\time 4/4\n");
        sb.append("    \\tempo 4 = 120\n");
        sb.append("    ");
        
        // Convert notes to LilyPond format
        if (notes != null && !notes.isEmpty()) {
            for (String note : notes) {
                // Simple conversion - would be more complex in reality
                sb.append(note.toLowerCase()).append(" ");
            }
        } else {
            // Add a default note if no notes are provided
            sb.append("c4 d4 e4 f4 g4 a4 b4 c'4");
            logger.warn("No notes provided, using default C major scale");
        }
        
        sb.append("\n  }\n");
        sb.append("  \\layout { }\n");
        sb.append("  \\midi { }\n");
        sb.append("}\n");
        
        return sb.toString();
    }
    
    /**
     * Compile LilyPond file to PDF
     * @param lilypondFilePath Path to the LilyPond file
     * @return true if compilation was successful
     */
    private boolean compileLilyPondToPdf(String lilypondFilePath) {
        try {
            // Get the output PDF path
            Path lilypondPath = Paths.get(lilypondFilePath);
            String fileName = lilypondPath.getFileName().toString().replace(".ly", ".pdf");
            Path pdfPath = outputDir.resolve(fileName);
            
            logger.info("Creating PDF at: {}", pdfPath.toAbsolutePath());
            
            // Create a simple PDF file for demonstration
            // In a real app, this would call the LilyPond executable
            String dummyPdfContent = "%PDF-1.5\n1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n" +
                    "2 0 obj\n<</Type/Pages/Kids[3 0 R]/Count 1>>\nendobj\n" +
                    "3 0 obj\n<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>\nendobj\n" +
                    "xref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000053 00000 n\n0000000102 00000 n\n" +
                    "trailer\n<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF";
            
            Files.write(pdfPath, dummyPdfContent.getBytes());
            
            // Verify the file was created
            boolean fileExists = Files.exists(pdfPath);
            logger.info("PDF file created successfully: {}", fileExists);
            if (fileExists) {
                logger.info("PDF file size: {} bytes", Files.size(pdfPath));
                logger.info("PDF file can be accessed at: /output/{}", fileName);
            }
            
            return fileExists;
        } catch (IOException e) {
            logger.error("Error creating PDF file: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate MIDI file from notes
     * @param recordingId The unique ID for this recording
     * @param notes The list of MIDI notes
     * @throws IOException If file operations fail
     */
    private void generateMidiFile(String recordingId, List<String> notes) throws IOException {
        // In a real app, you would generate a proper MIDI file
        // For this example, we'll create a dummy file
        Path midiPath = outputDir.resolve(recordingId + ".midi");
        
        // Create a simple MIDI file header
        byte[] midiHeader = {0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06, 
                            0x00, 0x01, 0x00, 0x01, 0x00, 0x60, 0x4D, 0x54, 
                            0x72, 0x6B, 0x00, 0x00, 0x00, 0x04, 0x00, (byte) 0xFF, 0x2F, 0x00};
        
        Files.write(midiPath, midiHeader);
        logger.info("MIDI file created at: {}", midiPath.toAbsolutePath());
    }
    
    /**
     * Check if a PDF file exists for the given recording ID
     * @param recordingId The recording ID
     * @return true if the PDF file exists
     */
    public boolean pdfExists(String recordingId) {
        Path pdfPath = outputDir.resolve(recordingId + ".pdf");
        boolean exists = Files.exists(pdfPath);
        logger.info("Checking if PDF exists for recording {}: {}", recordingId, exists);
        return exists;
    }
    
    /**
     * Get the absolute path to the PDF file
     * @param recordingId The recording ID
     * @return The absolute path to the PDF file
     */
    public String getPdfPath(String recordingId) {
        return outputDir.resolve(recordingId + ".pdf").toAbsolutePath().toString();
    }
    
    /**
     * Get the output directory path
     * @return The output directory path
     */
    public String getOutputDirectory() {
        return outputDir.toAbsolutePath().toString();
    }
}