package com.example.keyworks.controller;

import com.example.keyworks.service.MidiDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/midi")
public class MidiController {

    private static final Logger logger = LoggerFactory.getLogger(MidiController.class);
    
    @Autowired
    private MidiDeviceService midiDeviceService;

    /**
     * Get all available MIDI devices
     * @return List of available MIDI devices
     */
    @GetMapping("/devices")
    public ResponseEntity<Map<String, Object>> listDevices() {
        List<String> devices = midiDeviceService.listMidiDevices();
        Map<String, Object> response = new HashMap<>();
        response.put("devices", devices);
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint to get detailed information about all MIDI devices
     * @return Detailed information about all MIDI devices
     */
    @GetMapping("/debug-devices")
    public ResponseEntity<Map<String, Object>> debugDevices() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            List<Map<String, Object>> deviceDetails = new java.util.ArrayList<>();
            
            for (MidiDevice.Info info : infos) {
                Map<String, Object> deviceInfo = new HashMap<>();
                deviceInfo.put("name", info.getName());
                deviceInfo.put("description", info.getDescription());
                deviceInfo.put("vendor", info.getVendor());
                deviceInfo.put("version", info.getVersion());
                
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    deviceInfo.put("maxTransmitters", device.getMaxTransmitters());
                    deviceInfo.put("maxReceivers", device.getMaxReceivers());
                    deviceInfo.put("isOpen", device.isOpen());
                    
                    // Determine if it's an input or output device
                    if (device.getMaxTransmitters() != 0) {
                        deviceInfo.put("type", "Input");
                    } else if (device.getMaxReceivers() != 0) {
                        deviceInfo.put("type", "Output");
                    } else {
                        deviceInfo.put("type", "Unknown");
                    }
                    
                } catch (MidiUnavailableException e) {
                    deviceInfo.put("error", "Device unavailable: " + e.getMessage());
                }
                
                deviceDetails.add(deviceInfo);
            }
            
            response.put("devices", deviceDetails);
            response.put("count", deviceDetails.size());
            
        } catch (Exception e) {
            response.put("error", "Failed to get MIDI devices: " + e.getMessage());
            logger.error("Error in debug-devices endpoint", e);
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Start listening to a MIDI device
     * @param deviceName Name of the device to listen to
     * @return Status of the connection
     */
    @PostMapping("/listen")
    public ResponseEntity<Map<String, Object>> startListening(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String result = midiDeviceService.startListening(deviceName);
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (MidiUnavailableException e) {
            logger.error("Failed to start listening to MIDI device", e);
            response.put("status", "error");
            response.put("message", "Failed to connect to MIDI device: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Listen to a virtual MIDI device
     * @param deviceName Name of the virtual device
     * @return Status of the connection
     */
    @PostMapping("/listen-virtual")
    public ResponseEntity<Map<String, Object>> listenToVirtualDevice(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String result = midiDeviceService.listenToVirtualMidiInput(deviceName);
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to connect to virtual MIDI device", e);
            response.put("status", "error");
            response.put("message", "Failed to connect to virtual MIDI device: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Listen directly to a MIDI device
     * @param deviceName Name of the device
     * @return Status of the connection
     */
    @PostMapping("/listen-direct")
    public ResponseEntity<Map<String, Object>> listenDirectToDevice(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String result = midiDeviceService.listenDirectToDevice(deviceName);
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to connect directly to MIDI device", e);
            response.put("status", "error");
            response.put("message", "Failed to connect directly to MIDI device: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Stop listening to a MIDI device
     * @param deviceName Name of the device to stop listening to
     * @return Status of the operation
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopListening(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            midiDeviceService.stopListening(deviceName);
            response.put("status", "success");
            response.put("message", "Stopped listening to MIDI device: " + deviceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to stop listening to MIDI device", e);
            response.put("status", "error");
            response.put("message", "Failed to stop listening to MIDI device: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Stop listening to all MIDI devices
     * @return Status of the operation
     */
    @PostMapping("/stop-all")
    public ResponseEntity<Map<String, Object>> stopAll() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            midiDeviceService.stopAll();
            response.put("status", "success");
            response.put("message", "Stopped listening to all MIDI devices");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to stop listening to all MIDI devices", e);
            response.put("status", "error");
            response.put("message", "Failed to stop listening to all MIDI devices: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Start recording MIDI input
     * @param deviceName Name of the device to record from
     * @return Recording ID
     */
    @PostMapping("/record/start")
    public ResponseEntity<Map<String, Object>> startRecording(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String recordingId = midiDeviceService.startRecording(deviceName);
            response.put("status", "success");
            response.put("recordingId", recordingId);
            response.put("message", "Started recording from MIDI device: " + deviceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to start recording from MIDI device", e);
            response.put("status", "error");
            response.put("message", "Failed to start recording: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
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
        } catch (Exception e) {
            logger.error("Failed to stop recording", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to stop recording: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get recording data by ID
     * @param id Recording ID
     * @return Recording data
     */
    @GetMapping("/record/{id}")
    public ResponseEntity<Map<String, Object>> getRecordingData(@PathVariable String id) {
        Map<String, Object> recordingData = midiDeviceService.getRecordingData(id);
        
        if (recordingData != null) {
            return ResponseEntity.ok(recordingData);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Recording not found with ID: " + id);
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Test endpoint to simulate MIDI input
     * @param notes Array of MIDI note numbers to simulate
     * @return Recording ID
     */
    @PostMapping("/test-midi-input")
    public ResponseEntity<Map<String, Object>> testMidiInput(@RequestBody int[] notes) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String recordingId = midiDeviceService.simulateMidiInput(notes);
            response.put("status", "success");
            response.put("recordingId", recordingId);
            response.put("message", "Simulated MIDI input for " + notes.length + " notes");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to simulate MIDI input", e);
            response.put("status", "error");
            response.put("message", "Failed to simulate MIDI input: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Test endpoint to check if MIDI reception is working
     * @return Status of the test
     */
    @GetMapping("/test-midi-reception")
    public ResponseEntity<Map<String, Object>> testMidiReception() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Simulate a note on and note off event
            midiDeviceService.processNoteOn(60, 100, System.currentTimeMillis());
            Thread.sleep(500); // Wait 500ms
            midiDeviceService.processNoteOff(60, System.currentTimeMillis());
            
            response.put("status", "success");
            response.put("message", "MIDI reception test successful");
            response.put("noteProcessed", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to test MIDI reception", e);
            response.put("status", "error");
            response.put("message", "Failed to test MIDI reception: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Check if a device can be used for recording
     * @param deviceName Name of the device to check
     * @return True if the device can be used for recording
     */
    @GetMapping("/can-record")
    public ResponseEntity<Map<String, Object>> canDeviceRecord(@RequestParam String deviceName) {
        Map<String, Object> response = new HashMap<>();
        
        boolean canRecord = midiDeviceService.canDeviceRecord(deviceName);
        response.put("canRecord", canRecord);
        
        if (canRecord) {
            response.put("message", "Device can be used for recording");
        } else {
            response.put("message", "Device cannot be used for recording");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Direct endpoint for receiving MIDI events via HTTP
     * @param eventData MIDI event data
     * @return Status of the operation
     */
    @PostMapping("/direct-midi-event")
    public ResponseEntity<Map<String, Object>> receiveMidiEvent(@RequestBody Map<String, Object> eventData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String type = (String) eventData.get("type");
            int note = ((Number) eventData.get("note")).intValue();
            int velocity = ((Number) eventData.get("velocity")).intValue();
            long timestamp = ((Number) eventData.get("timestamp")).longValue();
            
            logger.info("Received direct MIDI event via HTTP: {} note: {} velocity: {}", 
                type, note, velocity);
            
            if ("note-on".equals(type)) {
                midiDeviceService.processNoteOn(note, velocity, timestamp);
                response.put("status", "success");
                response.put("message", "Processed Note On event");
            } else if ("note-off".equals(type)) {
                midiDeviceService.processNoteOff(note, timestamp);
                response.put("status", "success");
                response.put("message", "Processed Note Off event");
            } else {
                response.put("status", "error");
                response.put("message", "Unknown event type: " + type);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing direct MIDI event", e);
            response.put("status", "error");
            response.put("message", "Error processing MIDI event: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}