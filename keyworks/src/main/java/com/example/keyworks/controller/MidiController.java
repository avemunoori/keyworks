package com.example.keyworks.controller;

import com.example.keyworks.service.MidiDeviceService;
import com.example.keyworks.service.MidiProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for MIDI-related operations
 */
@RestController
@RequestMapping("/api/midi")
public class MidiController {

    private static final Logger logger = LoggerFactory.getLogger(MidiController.class);
    private final MidiDeviceService midiDeviceService;
    private final MidiProcessingService midiProcessingService;


    public MidiController(MidiDeviceService midiDeviceService, MidiProcessingService midiProcessingService) {
        this.midiDeviceService = midiDeviceService;
        this.midiProcessingService = midiProcessingService;
    }

    /**
     * List all available MIDI devices
     * @return List of MIDI device names
     */
    @GetMapping("/devices")
    public ResponseEntity<List<String>> listMidiDevices() {
        List<String> devices = midiDeviceService.listMidiDevices();
        logger.info("Found {} MIDI devices", devices.size());
        return ResponseEntity.ok(devices);
    }
    
    /**
     * Process MIDI data and generate sheet music
     * @param request Map containing recordingId and notes
     * @return Map with URLs to generated files
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processMidiData(@RequestBody Map<String, Object> request) {
        String recordingId = (String) request.get("recordingId");
        List<String> notes;
        
        try {
            notes = (List<String>) request.get("notes");
        } catch (ClassCastException e) {
            logger.error("Invalid notes format in request: {}", e.getMessage());
            notes = new ArrayList<>();
        }
        
        if (notes == null) {
            notes = new ArrayList<>();
        }
        
        logger.info("Processing MIDI data for recording ID: {} with {} notes", recordingId, notes.size());
        
        boolean success = midiProcessingService.processMidiToLilyPond(recordingId, notes);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("success", "true");
            response.put("recordingId", recordingId);
            response.put("pdfUrl", "/output/" + recordingId + ".pdf");
            response.put("midiUrl", "/output/" + recordingId + ".midi");
            response.put("lilypondUrl", "/output/" + recordingId + ".ly");
            
            logger.info("Successfully processed MIDI data. PDF available at: {}", response.get("pdfUrl"));
            return ResponseEntity.ok(response);
        } else {
            response.put("success", "false");
            response.put("error", "Failed to process MIDI data");
            
            logger.error("Failed to process MIDI data for recording ID: {}", recordingId);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Connect to a MIDI device
     * @param deviceName Name of the MIDI device to connect to
     * @return Success or failure message
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connectToDevice(@RequestBody Map<String, String> request) {
        String deviceName = request.get("deviceName");
        
        if (deviceName == null || deviceName.isEmpty()) {
            logger.error("No device name provided for connection");
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "error", "No device name provided"
            ));
        }
        
        logger.info("Attempting to connect to MIDI device: {}", deviceName);
        boolean connected = false;
        
        try {
            String result = midiDeviceService.startListening(deviceName);
            connected = result != null && !result.isEmpty();
        } catch (Exception e) {
            logger.error("Error connecting to device: {}", e.getMessage());
        }
        
        if (connected) {
            logger.info("Successfully connected to MIDI device: {}", deviceName);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Connected to " + deviceName
            ));
        } else {
            logger.error("Failed to connect to MIDI device: {}", deviceName);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", "false",
                "error", "Failed to connect to " + deviceName
            ));
        }
    }
    
    /**
     * Disconnect from the current MIDI device
     * @return Success or failure message
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnectDevice() {
        logger.info("Disconnecting from MIDI device");
        midiDeviceService.stopAll();
        
        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Disconnected from MIDI device"
        ));
    }
    
    /**
     * Start recording MIDI notes
     * @return Success or failure message
     */
    @PostMapping("/record/start")
    public ResponseEntity<Map<String, String>> startRecording(@RequestBody(required = false) Map<String, String> request) {
        logger.info("Starting MIDI recording");
        String deviceName = request != null ? request.get("deviceName") : null;
        
        try {
            String recordingId;
            if (deviceName != null && !deviceName.isEmpty()) {
                recordingId = midiDeviceService.startRecording(deviceName);
            } else {
                // Use the first available device if none specified
                List<String> devices = midiDeviceService.listMidiDevices();
                if (devices.isEmpty()) {
                    return ResponseEntity.internalServerError().body(Map.of(
                        "success", "false",
                        "error", "No MIDI devices available"
                    ));
                }
                recordingId = midiDeviceService.startRecording(devices.get(0));
            }
            
            logger.info("MIDI recording started successfully with ID: {}", recordingId);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Recording started",
                "recordingId", recordingId
            ));
        } catch (Exception e) {
            logger.error("Failed to start MIDI recording: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", "false",
                "error", "Failed to start recording: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Stop recording MIDI notes and return the recorded notes
     * @return Recording data including processed files
     */
    @PostMapping("/record/stop")
    public ResponseEntity<Map<String, Object>> stopRecording() {
        logger.info("Stopping MIDI recording");
        
        try {
            // Get the recording data from the service
            Map<String, Object> recordingData = midiDeviceService.stopRecording();
            
            // Create response with success status
            Map<String, Object> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Recording stopped");
            
            // Add recording data to response
            if (recordingData != null) {
                response.put("recordingId", recordingData.get("id"));
                response.put("noteCount", recordingData.get("noteCount"));
                response.put("duration", recordingData.get("duration"));
                response.put("lilyPondCode", recordingData.get("lilyPondCode"));
                
                // Process the recording to generate PDF, MIDI, etc.
                String recordingId = (String) recordingData.get("id");
                List<MidiDeviceService.MidiNote> midiNotes = (List<MidiDeviceService.MidiNote>) recordingData.get("notes");
                
                // Convert MidiNote objects to simple note names for the processing service
                List<String> noteNames = new ArrayList<>();
                if (midiNotes != null) {
                    for (MidiDeviceService.MidiNote note : midiNotes) {
                        // Convert MIDI note number to note name
                        noteNames.add(convertMidiNoteToName(note.getKey()));
                    }
                }
                
                // Process the notes to generate files
                boolean success = midiProcessingService.processMidiToLilyPond(recordingId, noteNames);
                
                if (success) {
                    response.put("pdfUrl", "/output/" + recordingId + ".pdf");
                    response.put("midiUrl", "/output/" + recordingId + ".midi");
                    response.put("lilypondUrl", "/output/" + recordingId + ".ly");
                }
            }
            
            logger.info("MIDI recording stopped and processed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error stopping recording: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", "false",
                "error", "Failed to stop recording: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Simulate a MIDI note event
     * @param request Map containing note and velocity
     * @return Success or failure message
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, String>> simulateNote(@RequestBody Map<String, Object> request) {
        String note = (String) request.get("note");
        Integer velocity = (Integer) request.get("velocity");
        Boolean noteOn = (Boolean) request.get("noteOn");
        
        if (note == null || velocity == null || noteOn == null) {
            logger.error("Invalid parameters for note simulation");
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "error", "Missing required parameters"
            ));
        }
        
        logger.info("Simulating MIDI note: {} (velocity: {}, on: {})", note, velocity, noteOn);
        midiDeviceService.simulateNote(note, velocity, noteOn);
        
        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Note simulated: " + note
        ));
    }
    
    /**
     * Get the status of the MIDI device connection
     * @return Connection status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean connected = midiDeviceService.isConnected();
        boolean recording = midiDeviceService.isRecording();
        String currentDevice = midiDeviceService.getCurrentDevice();
        
        Map<String, Object> status = new HashMap<>();
        status.put("connected", connected);
        status.put("recording", recording);
        status.put("currentDevice", currentDevice != null ? currentDevice : "");
        
        logger.info("MIDI status: connected={}, recording={}, device={}", 
                    connected, recording, currentDevice);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Convert MIDI note number to note name
     * @param noteNumber MIDI note number
     * @return Note name (e.g., "C4")
     */
    private String convertMidiNoteToName(int noteNumber) {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = (noteNumber / 12) - 1;
        int note = noteNumber % 12;
        return noteNames[note] + octave;
    }
}