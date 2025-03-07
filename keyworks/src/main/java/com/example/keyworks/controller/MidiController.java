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
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/midi")
@CrossOrigin(origins = "*")
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
        logger.info("Fetching MIDI devices...");
        List<MidiDevice.Info> deviceInfos = midiProcessingService.getMidiDevices();
        List<Map<String, String>> devices = new ArrayList<>();
        
        for (MidiDevice.Info info : deviceInfos) {
            Map<String, String> device = new HashMap<>();
            String deviceId = UUID.nameUUIDFromBytes(info.getName().getBytes()).toString();
            
            device.put("id", deviceId);
            device.put("name", info.getName());
            device.put("description", info.getDescription());
            device.put("vendor", info.getVendor());
            device.put("version", info.getVersion());
            
            // Add device type information
            try {
                MidiDevice midiDevice = MidiSystem.getMidiDevice(info);
                boolean isInput = midiDevice.getMaxTransmitters() != 0;
                boolean isOutput = midiDevice.getMaxReceivers() != 0;
                String type = "";
                
                if (isInput) type += "Input";
                if (isOutput) {
                    if (!type.isEmpty()) type += ", ";
                    type += "Output";
                }
                
                device.put("type", type);
                device.put("isVirtual", info.getName().toLowerCase().contains("virtual") ? "Virtual" : "Physical");
            } catch (MidiUnavailableException e) {
                logger.warn("Could not get additional info for device {}: {}", info.getName(), e.getMessage());
                device.put("type", "Unknown");
                device.put("isVirtual", "Unknown");
            }
            
            devices.add(device);
        }
        
        logger.info("Found {} MIDI devices", devices.size());
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
        
        logger.info("Attempting to connect to MIDI device with ID: {}", deviceId);
        List<MidiDevice.Info> deviceInfos = midiProcessingService.getMidiDevices();
        
        for (MidiDevice.Info info : deviceInfos) {
            String id = UUID.nameUUIDFromBytes(info.getName().getBytes()).toString();
            if (id.equals(deviceId)) {
                try {
                    boolean success = midiProcessingService.connectToDevice(info);
                    if (success) {
                        logger.info("Successfully connected to MIDI device: {}", info.getName());
                        response.put("success", true);
                        response.put("message", "Connected to MIDI device: " + info.getName());
                        return ResponseEntity.ok(response);
                    } else {
                        logger.error("Failed to connect to MIDI device: {}", info.getName());
                        response.put("success", false);
                        response.put("message", "Failed to connect to MIDI device: " + info.getName());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                } catch (Exception e) {
                    logger.error("Error connecting to MIDI device {}: {}", info.getName(), e.getMessage(), e);
                    response.put("success", false);
                    response.put("message", "Error connecting to MIDI device: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }
        }
        
        logger.error("MIDI device not found with ID: {}", deviceId);
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
        
        try {
            logger.info("Disconnecting from MIDI device");
            midiProcessingService.disconnectFromDevice();
            
            response.put("success", true);
            response.put("message", "Disconnected from MIDI device");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error disconnecting from MIDI device: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error disconnecting from MIDI device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Starts recording MIDI input
     * @return The ID of the recording session
     */
    @PostMapping("/record/start")
    public ResponseEntity<Map<String, Object>> startRecording() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Starting MIDI recording");
            String recordingId = midiProcessingService.startRecording();
            
            if (recordingId != null) {
                response.put("success", true);
                response.put("recordingId", recordingId);
                response.put("message", "Recording started");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Failed to start recording. No MIDI device connected.");
                response.put("success", false);
                response.put("message", "Failed to start recording. Make sure a MIDI device is connected.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error starting recording: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error starting recording: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Stops the current recording session
     * @return The list of recorded notes
     */
    @PostMapping("/record/stop")
    public ResponseEntity<Map<String, Object>> stopRecording() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Stopping MIDI recording");
            List<String> notes = midiProcessingService.stopRecording();
            
            response.put("success", true);
            response.put("message", "Recording stopped");
            response.put("notes", notes);
            response.put("count", notes.size());
            response.put("recordingId", midiProcessingService.getCurrentRecordingId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error stopping recording: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error stopping recording: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Simulates playing a C major scale
     * @return Success message
     */
    @PostMapping("/simulate/scale")
    public ResponseEntity<Map<String, Object>> simulateCMajorScale() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!midiProcessingService.isRecording()) {
                logger.warn("Cannot simulate scale: Not recording");
                response.put("success", false);
                response.put("message", "Please start recording first");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            logger.info("Simulating C major scale");
            midiProcessingService.simulateCMajorScale();
            
            response.put("success", true);
            response.put("message", "C major scale simulated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error simulating scale: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error simulating scale: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Generates a PDF from the recorded notes
     * @return The URL to access the generated PDF
     */
    @PostMapping("/generate/pdf")
    public ResponseEntity<Map<String, Object>> generatePDF() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Generating PDF from recorded notes");
            String pdfPath = midiProcessingService.generatePDFFromRecording();
            
            if (pdfPath != null) {
                String pdfUrl = "/api/files/output/" + pdfPath;
                logger.info("PDF generated successfully: {}", pdfUrl);
                
                response.put("success", true);
                response.put("message", "PDF generated successfully");
                response.put("pdfUrl", pdfUrl);
                response.put("recordingId", midiProcessingService.getCurrentRecordingId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Failed to generate PDF: No notes recorded");
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
            logger.info("Testing PDF generation with ID: {}", id);
            String pdfPath = midiProcessingService.testPdfGeneration(id);
            
            if (pdfPath != null) {
                String pdfUrl = "/api/files/output/" + pdfPath;
                logger.info("Test PDF generated successfully: {}", pdfUrl);
                
                response.put("success", true);
                response.put("message", "PDF test generation completed");
                response.put("pdfUrl", pdfUrl);
                response.put("testId", id);
                return ResponseEntity.ok(response);
            } else {
                logger.error("Failed to generate test PDF");
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
        
        try {
            boolean isRecording = midiProcessingService.isRecording();
            String recordingId = midiProcessingService.getCurrentRecordingId();
            int noteCount = midiProcessingService.getRecordedNotes().size();
            
            response.put("isRecording", isRecording);
            response.put("recordingId", recordingId);
            response.put("noteCount", noteCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting status: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error getting status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Gets the recorded notes
     * @return The list of recorded notes
     */
    @GetMapping("/notes")
    public ResponseEntity<Map<String, Object>> getRecordedNotes() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> notes = midiProcessingService.getRecordedNotes();
            
            response.put("notes", notes);
            response.put("count", notes.size());
            response.put("recordingId", midiProcessingService.getCurrentRecordingId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting recorded notes: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error getting recorded notes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Downloads the generated PDF
     * @param recordingId The ID of the recording
     * @return The PDF file
     */
    @GetMapping("/download/pdf/{recordingId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String recordingId) {
        try {
            logger.info("Downloading PDF for recording ID: {}", recordingId);
            String pdfFileName = "music_generated_" + recordingId + ".pdf";
            
            // Try to load the file as a resource
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
            
            logger.info("Serving PDF download for recording ID: {}", recordingId);
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
            logger.info("Viewing PDF for recording ID: {}", recordingId);
            String pdfFileName = "music_generated_" + recordingId + ".pdf";
            
            // Try to load the file as a resource
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
            
            logger.info("Serving PDF view for recording ID: {}", recordingId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to view PDF: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Plays a virtual MIDI note
     * @param note The note to play (e.g., "C4")
     * @return Success message
     */
    @PostMapping("/play/{note}")
    public ResponseEntity<Map<String, Object>> playNote(@PathVariable String note) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Playing virtual MIDI note: {}", note);
            
            // Check if recording is active
            if (!midiProcessingService.isRecording()) {
                logger.warn("Cannot play note: Not recording");
                response.put("success", false);
                response.put("message", "Please start recording first");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Simulate the note in the MIDI processing service
            midiProcessingService.simulateNote(note);
            
            response.put("success", true);
            response.put("message", "Note played: " + note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error playing note {}: {}", note, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error playing note: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Gets a list of available MIDI devices with detailed information
     * @return List of MIDI device info objects with detailed information
     */
    @GetMapping("/devices/detailed")
    public ResponseEntity<List<Map<String, Object>>> getMidiDevicesDetailed() {
        logger.info("Fetching detailed MIDI device information...");
        List<MidiDevice.Info> deviceInfos = midiProcessingService.getMidiDevices();
        List<Map<String, Object>> devices = new ArrayList<>();
        
        for (MidiDevice.Info info : deviceInfos) {
            Map<String, Object> device = new HashMap<>();
            String deviceId = UUID.nameUUIDFromBytes(info.getName().getBytes()).toString();
            
            device.put("id", deviceId);
            device.put("name", info.getName());
            device.put("description", info.getDescription());
            device.put("vendor", info.getVendor());
            device.put("version", info.getVersion());
            
            // Add detailed device information
            try {
                MidiDevice midiDevice = MidiSystem.getMidiDevice(info);
                device.put("maxReceivers", midiDevice.getMaxReceivers());
                device.put("maxTransmitters", midiDevice.getMaxTransmitters());
                device.put("isOpen", midiDevice.isOpen());
                
                boolean isInput = midiDevice.getMaxTransmitters() != 0;
                boolean isOutput = midiDevice.getMaxReceivers() != 0;
                
                Map<String, Boolean> capabilities = new HashMap<>();
                capabilities.put("input", isInput);
                capabilities.put("output", isOutput);
                capabilities.put("virtual", info.getName().toLowerCase().contains("virtual"));
                
                device.put("capabilities", capabilities);
            } catch (MidiUnavailableException e) {
                logger.warn("Could not get detailed info for device {}: {}", info.getName(), e.getMessage());
                device.put("error", "Device unavailable: " + e.getMessage());
            }
            
            devices.add(device);
        }
        
        logger.info("Found {} MIDI devices with detailed information", devices.size());
        return ResponseEntity.ok(devices);
    }
    
   
}