package com.example.keyworks.controller;

import com.example.keyworks.service.MidiDeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.MidiUnavailableException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/midi")
public class MidiController {

    private final MidiDeviceService midiDeviceService;
    
    public MidiController(MidiDeviceService midiDeviceService) {
        this.midiDeviceService = midiDeviceService;
    }
    
    /**
     * Lists all available MIDI devices
     * @return List of MIDI devices
     */
    @GetMapping("/devices")
    public ResponseEntity<List<String>> listMidiDevices() {
        return ResponseEntity.ok(midiDeviceService.listMidiDevices());
    }
    
    /**
     * Starts listening to a MIDI device
     * @param deviceName Name of the device to listen to
     * @return Status message
     */
    @PostMapping("/listen/{deviceName}")
    public ResponseEntity<?> listenToDevice(@PathVariable String deviceName) {
        try {
            String result = midiDeviceService.startListening(deviceName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Now listening for MIDI input from " + deviceName,
                "device", result
            ));
        } catch (MidiUnavailableException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "MIDI device unavailable: " + e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Stops listening to a MIDI device
     * @param deviceName Name of the device to stop listening to
     * @return Status message
     */
    @PostMapping("/stop/{deviceName}")
    public ResponseEntity<?> stopListeningToDevice(@PathVariable String deviceName) {
        midiDeviceService.stopListening(deviceName);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Stopped listening to " + deviceName
        ));
    }
    
    /**
     * Stops listening to all MIDI devices
     * @return Status message
     */
    @PostMapping("/stop-all")
    public ResponseEntity<?> stopAllDevices() {
        midiDeviceService.stopAll();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Stopped listening to all MIDI devices"
        ));
    }
}