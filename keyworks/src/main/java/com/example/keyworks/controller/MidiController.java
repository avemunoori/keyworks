package com.example.keyworks.controller;

import com.example.keyworks.service.MidiDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.MidiUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/midi")
public class MidiController {

    private static final Logger logger = LoggerFactory.getLogger(MidiController.class);

    private final MidiDeviceService midiDeviceService;

    public MidiController(MidiDeviceService midiDeviceService) {
        this.midiDeviceService = midiDeviceService;
    }

    /**
     * List all available MIDI devices
     * @return List of MIDI devices
     */
    @GetMapping("/devices")
    public ResponseEntity<List<String>> listMidiDevices() {
        List<String> devices = midiDeviceService.listMidiDevices();
        return ResponseEntity.ok(devices);
    }

    /**
     * Start recording from a MIDI device
     * @param deviceName Name of the device to record from
     * @return Recording ID
     */
    @PostMapping("/record/start")
    public ResponseEntity<Map<String, Object>> startRecording(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String recordingId = midiDeviceService.startRecording(deviceName);
            response.put("status", "success");
            response.put("message", "Started recording from device: " + deviceName);
            response.put("recordingId", recordingId);
            return ResponseEntity.ok(response);
        } catch (MidiUnavailableException e) {
            logger.error("Failed to start recording", e);
            response.put("status", "error");
            response.put("message", "Failed to start recording: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Stop recording MIDI input
     * @return Recording data
     */
    @PostMapping("/record/stop")
    public ResponseEntity<Map<String, Object>> stopRecording() {
        try {
            Map<String, Object> recordingData = midiDeviceService.stopRecording();
            return ResponseEntity.ok(recordingData);
        } catch (IllegalStateException e) {
            logger.error("Failed to stop recording", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get recording data by ID
     * @param id Recording ID
     * @return Recording data
     */
    @GetMapping("/recording/{id}")
    public ResponseEntity<Map<String, Object>> getRecording(@PathVariable String id) {
        Map<String, Object> recordingData = midiDeviceService.getRecordingData(id);
        
        if (recordingData == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(recordingData);
    }

    /**
     * Process a direct MIDI event from HTTP
     * @param isNoteOn Whether this is a note on event
     * @param note MIDI note number
     * @param velocity Velocity (0-127)
     * @return Status message
     */
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> processMidiEvent(
            @RequestParam boolean isNoteOn,
            @RequestParam int note,
            @RequestParam(defaultValue = "64") int velocity) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String result = midiDeviceService.processDirectMidiEvent(isNoteOn, note, velocity);
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to process MIDI event", e);
            response.put("status", "error");
            response.put("message", "Failed to process MIDI event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Simulate MIDI input for testing
     * @param notes Array of MIDI note numbers to simulate
     * @return Recording ID
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateMidiInput(@RequestBody int[] notes) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String recordingId = midiDeviceService.simulateMidiInput(notes);
            response.put("status", "success");
            response.put("message", "Simulated MIDI input with " + notes.length + " notes");
            response.put("recordingId", recordingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to simulate MIDI input", e);
            response.put("status", "error");
            response.put("message", "Failed to simulate MIDI input: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Set the current user for recording
     * @param userId ID of the user who is recording
     * @return Status of the operation
     */
    @PostMapping("/set-user")
    public ResponseEntity<Map<String, Object>> setCurrentUser(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            midiDeviceService.setCurrentUserId(userId);
            response.put("status", "success");
            response.put("message", "Set current user ID to: " + userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to set current user", e);
            response.put("status", "error");
            response.put("message", "Failed to set current user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check if a device can be used for recording
     * @param deviceName Name of the device to check
     * @return Status of the device
     */
    @GetMapping("/device/check")
    public ResponseEntity<Map<String, Object>> checkDevice(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        boolean canRecord = midiDeviceService.canDeviceRecord(deviceName);
        response.put("deviceName", deviceName);
        response.put("canRecord", canRecord);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Connect to a specific MIDI device
     * @param deviceName Name of the device to connect to
     * @return Status of the connection
     */
    @PostMapping("/device/connect")
    public ResponseEntity<Map<String, Object>> connectToDevice(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String result = midiDeviceService.startListening(deviceName);
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to connect to device", e);
            response.put("status", "error");
            response.put("message", "Failed to connect to device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Disconnect from a MIDI device
     * @param deviceName Name of the device to disconnect from
     * @return Status of the operation
     */
    @PostMapping("/device/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectFromDevice(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            midiDeviceService.stopListening(deviceName);
            response.put("status", "success");
            response.put("message", "Disconnected from device: " + deviceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to disconnect from device", e);
            response.put("status", "error");
            response.put("message", "Failed to disconnect from device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Disconnect from all MIDI devices
     * @return Status of the operation
     */
    @PostMapping("/device/disconnect-all")
    public ResponseEntity<Map<String, Object>> disconnectFromAllDevices() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            midiDeviceService.stopAll();
            response.put("status", "success");
            response.put("message", "Disconnected from all devices");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to disconnect from all devices", e);
            response.put("status", "error");
            response.put("message", "Failed to disconnect from all devices: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}