package com.example.keyworks.controller;

import com.example.keyworks.service.MidiDeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/midi")
public class MidiController {

    private final MidiDeviceService midiDeviceService;


    public MidiController(MidiDeviceService midiDeviceService) {
        this.midiDeviceService = midiDeviceService;
    }

    @GetMapping("/devices")
    public ResponseEntity<List<Map<String, String>>> getAvailableMidiDevices() {
        List<Map<String, String>> devices = midiDeviceService.getAvailableInputDevices().stream()
            .map(device -> {
                Map<String, String> deviceInfo = new HashMap<>();
                deviceInfo.put("id", device.getDeviceInfo().getName());
                deviceInfo.put("name", device.getDeviceInfo().getName());
                return deviceInfo;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(devices);
    }

    @PostMapping("/record/start")
    public ResponseEntity<Map<String, String>> startRecording(@RequestParam String deviceName) {
        try {
            String recordingId = midiDeviceService.startRecording(deviceName);
            Map<String, String> response = new HashMap<>();
            response.put("status", "recording");
            response.put("recordingId", recordingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Replace Map.of with HashMap for compatibility with older Java versions
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    @PostMapping("/record/stop")
    public ResponseEntity<Map<String, Object>> stopRecording() {
        try {
            Map<String, Object> recordingData = midiDeviceService.stopRecording();
            return ResponseEntity.ok(recordingData);
        } catch (Exception e) {
            // Replace Map.of with HashMap for compatibility with older Java versions
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    @GetMapping("/recording/{id}")
    public ResponseEntity<Map<String, Object>> getRecording(@PathVariable String id) {
        try {
            Map<String, Object> recordingData = midiDeviceService.getRecordingData(id);
            if (recordingData == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(recordingData);
        } catch (Exception e) {
            // Replace Map.of with HashMap for compatibility with older Java versions
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }
}