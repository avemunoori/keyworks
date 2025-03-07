package com.example.keyworks.controller;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.service.FileService;
import com.example.keyworks.service.MidiProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.MidiDevice;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/midi")
public class MidiController {
    private static final Logger logger = LoggerFactory.getLogger(MidiController.class);
    
    private final MidiProcessingService midiProcessingService;
    private final FileService fileService;
    private final FileStorageConfig fileStorageConfig;
    
    public MidiController(MidiProcessingService midiProcessingService, FileService fileService, FileStorageConfig fileStorageConfig) {
        this.midiProcessingService = midiProcessingService;
        this.fileService = fileService;
        this.fileStorageConfig = fileStorageConfig;
        logger.info("MidiController initialized");
    }
    
    /**
     * Gets a list of available MIDI devices
     * @return List of MIDI device info objects
     */
    @GetMapping("/devices")
    public ResponseEntity<List<Map<String, String>>> getMidiDevices() {
        List<MidiDevice.Info> deviceInfos = midiProcessingService.getMidiDevices();
        List<Map<String, String>> devices = new ArrayList<>();
        
        for (MidiDevice.Info info : deviceInfos) {
            Map<String, String> device = new HashMap<>();
            device.put("name", info.getName());
            device.put("description", info.getDescription());
            device.put("vendor", info.getVendor());
            device.put("version", info.getVersion());
            device.put("id", UUID.nameUUIDFromBytes(info.getName().getBytes()).toString());
            devices.add(device);
        }
        
        return ResponseEntity.ok(devices);
    }
    
    /**
     * Connects to a MIDI device
     * @param deviceId The ID of the device to connect to
     * @return Success or failure message
     */
    @PostMapping("/connect/{deviceId}")
    public ResponseEntity<Map<String, Object>> connectToDevice(@PathVariable String deviceId) {
        Map<String, Object> response = new HashMap<>();
        
        List<MidiDevice.Info> deviceInfos = midiProcessingService.getMidiDevices();
        for (MidiDevice.Info info : deviceInfos) {
            String id = UUID.nameUUIDFromBytes(info.getName().getBytes()).toString();
            if (id.equals(deviceId)) {
                boolean success = midiProcessingService.connectToDevice(info);
                if (success) {
                    response.put("success", true);
                    response.put("message", "Connected to MIDI device: " + info.getName());
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("message", "Failed to connect to MIDI device: " + info.getName());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }
        }
        
        response.put("success", false);
        response.put("message", "MIDI device not found with ID: " + deviceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Disconnects from the current MIDI device
     * @return Success message
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectFromDevice() {
        Map<String, Object> response = new HashMap<>();
        
        midiProcessingService.disconnectFromDevice();
        
        response.put("success", true);
        response.put("message", "Disconnected from MIDI device");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Starts recording MIDI input
     * @return The ID of the recording session
     */
    @PostMapping("/record/start")
    public ResponseEntity<Map<String, Object>> startRecording() {
        Map<String, Object> response = new HashMap<>();
        
        String recordingId = midiProcessingService.startRecording();
        if (recordingId != null) {
            response.put("success", true);
            response.put("recordingId", recordingId);
            response.put("message", "Recording started");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Failed to start recording. Make sure a MIDI device is connected.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Stops the current recording session
     * @return The list of recorded notes
     */
    @PostMapping("/record/stop")
    public ResponseEntity<Map<String, Object>> stopRecording() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> notes = midiProcessingService.stopRecording();
        
        response.put("success", true);
        response.put("message", "Recording stopped");
        response.put("notes", notes);
        response.put("count", notes.size());
        response.put("recordingId", midiProcessingService.getCurrentRecordingId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simulates playing a C major scale
     * @return Success message
     */
    @PostMapping("/simulate/scale")
    public ResponseEntity<Map<String, Object>> simulateCMajorScale() {
        Map<String, Object> response = new HashMap<>();
        
        if (!midiProcessingService.isRecording()) {
            response.put("success", false);
            response.put("message", "Please start recording first");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        midiProcessingService.simulateCMajorScale();
        
        response.put("success", true);
        response.put("message", "C major scale simulated");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generates a PDF from the recorded notes
     * @return The URL to access the generated PDF
     */
    @PostMapping("/generate/pdf")
    public ResponseEntity<Map<String, Object>> generatePDF() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String pdfPath = midiProcessingService.generatePDFFromRecording();
            if (pdfPath != null) {
                response.put("success", true);
                response.put("message", "PDF generated successfully");
                response.put("pdfUrl", "/api/files/output/" + pdfPath);
                response.put("recordingId", midiProcessingService.getCurrentRecordingId());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to generate PDF. No notes recorded.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error generating PDF: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error generating PDF: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Tests PDF generation with a simple scale
     * @return The URL to access the generated PDF
     */
    @PostMapping("/test/pdf/{id}")
    public ResponseEntity<Map<String, Object>> testPdfGeneration(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String pdfPath = midiProcessingService.testPdfGeneration(id);
            if (pdfPath != null) {
                response.put("success", true);
                response.put("message", "PDF test generation completed");
                response.put("pdfUrl", "/api/files/output/" + pdfPath);
                response.put("testId", id);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to generate test PDF");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            logger.error("Error in test PDF generation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error in test PDF generation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Gets the current recording status
     * @return The recording status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("isRecording", midiProcessingService.isRecording());
        response.put("recordingId", midiProcessingService.getCurrentRecordingId());
        response.put("noteCount", midiProcessingService.getRecordedNotes().size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets the recorded notes
     * @return The list of recorded notes
     */
    @GetMapping("/notes")
    public ResponseEntity<Map<String, Object>> getRecordedNotes() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> notes = midiProcessingService.getRecordedNotes();
        
        response.put("notes", notes);
        response.put("count", notes.size());
        response.put("recordingId", midiProcessingService.getCurrentRecordingId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Downloads the generated PDF
     * @param recordingId The ID of the recording
     * @return The PDF file
     */
    @GetMapping("/download/pdf/{recordingId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String recordingId) {
        try {
            String pdfFileName = "music_generated_" + recordingId + ".pdf";
            Resource resource = fileService.loadFileAsResource(pdfFileName);
            
            // Check if the file exists
            if (!resource.exists()) {
                logger.error("PDF not found for recording ID: {}", recordingId);
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType;
            try {
                Path path = fileStorageConfig.resolveFilePath(pdfFileName);
                contentType = Files.probeContentType(path);
            } catch (IOException e) {
                contentType = "application/pdf";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to download PDF: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Views the generated PDF
     * @param recordingId The ID of the recording
     * @return The PDF file for viewing
     */
    @GetMapping("/view/pdf/{recordingId}")
    public ResponseEntity<Resource> viewPdf(@PathVariable String recordingId) {
        try {
            String pdfFileName = "music_generated_" + recordingId + ".pdf";
            Resource resource = fileService.loadFileAsResource(pdfFileName);
            
            // Check if the file exists
            if (!resource.exists()) {
                logger.error("PDF not found for recording ID: {}", recordingId);
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType;
            try {
                Path path = fileStorageConfig.resolveFilePath(pdfFileName);
                contentType = Files.probeContentType(path);
            } catch (IOException e) {
                contentType = "application/pdf";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to view PDF: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}