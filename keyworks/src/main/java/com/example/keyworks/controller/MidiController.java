package com.example.keyworks.controller;

import com.example.keyworks.model.Note;
import com.example.keyworks.service.MidiDeviceService;
import com.example.keyworks.service.MidiProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        
        // Clear any existing notes
        midiProcessingService.clearNotes();
        
        // Add the notes to the processing service
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < notes.size(); i++) {
            String noteName = notes.get(i);
            int midiNote = convertNoteNameToMidi(noteName);
            int velocity = 64; // Medium velocity
            long noteTimestamp = timestamp + (i * 500); // 500ms between notes
            long duration = 400; // 400ms duration
            
            midiProcessingService.processNoteOn(midiNote, velocity, noteTimestamp);
            midiProcessingService.processNoteOff(midiNote, noteTimestamp + duration);
        }
        
        // Generate LilyPond file and compile to PDF using the recording ID
        String baseFilename = midiProcessingService.generateSheetMusic(recordingId);
        boolean success = (baseFilename != null);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("success", "true");
            response.put("recordingId", recordingId);
            response.put("pdfUrl", "/output/" + baseFilename + ".pdf");
            response.put("midiUrl", "/output/" + baseFilename + ".midi");
            response.put("lilypondUrl", "/output/" + baseFilename + ".ly");
            
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
            
            // Start recording in the processing service
            midiProcessingService.startRecording();
            
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
            
            // Stop recording in the processing service
            midiProcessingService.stopRecording();
            
            // Create response with success status
            Map<String, Object> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Recording stopped");
            
            // Add recording data to response
            if (recordingData != null) {
                String recordingId = (String) recordingData.get("id");
                response.put("recordingId", recordingId);
                response.put("noteCount", recordingData.get("noteCount"));
                response.put("duration", recordingData.get("duration"));
                response.put("lilyPondCode", recordingData.get("lilyPondCode"));
                
                // Generate sheet music using the recording ID as the filename
                String baseFilename = midiProcessingService.generateSheetMusic(recordingId);
                
                if (baseFilename != null) {
                    response.put("pdfUrl", "/output/" + baseFilename + ".pdf");
                    response.put("midiUrl", "/output/" + baseFilename + ".midi");
                    response.put("lilypondUrl", "/output/" + baseFilename + ".ly");
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
        
        // Also process the note in the MidiProcessingService if recording
        if (midiProcessingService.isRecording()) {
            int midiNote = convertNoteNameToMidi(note);
            long timestamp = System.currentTimeMillis();
            
            if (noteOn) {
                midiProcessingService.processNoteOn(midiNote, velocity, timestamp);
            } else {
                midiProcessingService.processNoteOff(midiNote, timestamp);
            }
        }
        
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
    
    /**
     * Convert note name to MIDI note number
     * @param noteName Note name (e.g., "C4")
     * @return MIDI note number
     */
    private int convertNoteNameToMidi(String noteName) {
        if (noteName == null || noteName.length() < 2) {
            return 60; // Default to middle C
        }
        
        // Define the mapping of note names to their values using HashMap
        Map<String, Integer> noteMap = new HashMap<>();
        noteMap.put("C", 0);
        noteMap.put("C#", 1);
        noteMap.put("Db", 1);
        noteMap.put("D", 2);
        noteMap.put("D#", 3);
        noteMap.put("Eb", 3);
        noteMap.put("E", 4);
        noteMap.put("F", 5);
        noteMap.put("F#", 6);
        noteMap.put("Gb", 6);
        noteMap.put("G", 7);
        noteMap.put("G#", 8);
        noteMap.put("Ab", 8);
        noteMap.put("A", 9);
        noteMap.put("A#", 10);
        noteMap.put("Bb", 10);
        noteMap.put("B", 11);
        
        // Extract note and octave
        String note = noteName.substring(0, noteName.length() - 1);
        int octave;
        try {
            octave = Integer.parseInt(noteName.substring(noteName.length() - 1));
        } catch (NumberFormatException e) {
            octave = 4; // Default to middle octave
        }
        
        // Calculate MIDI note number
        Integer noteValue = noteMap.get(note);
        if (noteValue == null) {
            return 60; // Default to middle C
        }
        
        return (octave + 1) * 12 + noteValue;
    }
}