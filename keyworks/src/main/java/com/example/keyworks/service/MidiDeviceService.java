package com.example.keyworks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MidiDeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(MidiDeviceService.class);
    private final Map<String, MidiDevice> openDevices = new ConcurrentHashMap<>();
    
    /**
     * Lists all available MIDI devices
     * @return List of device information strings
     */
    public List<String> listMidiDevices() {
        List<String> deviceList = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        
        logger.info("Found {} MIDI devices", infos.length);
        
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                String deviceType = "";
                
                if (device.getMaxTransmitters() != 0) {
                    deviceType += "Input";
                }
                
                if (device.getMaxReceivers() != 0) {
                    if (!deviceType.isEmpty()) {
                        deviceType += "/";
                    }
                    deviceType += "Output";
                }
                
                deviceList.add(String.format("%s - %s (%s)", 
                    info.getName(), 
                    info.getDescription(),
                    deviceType));
                
                logger.info("Device: {} - {} ({})", 
                    info.getName(), 
                    info.getDescription(),
                    deviceType);
                
            } catch (MidiUnavailableException e) {
                logger.error("Error accessing MIDI device: {}", info.getName(), e);
                deviceList.add(String.format("%s - %s (Unavailable)", 
                    info.getName(), 
                    info.getDescription()));
            }
        }
        
        return deviceList;
    }
    
    /**
     * Starts listening to a MIDI device
     * @param deviceName Name of the device to listen to
     * @return Information about the connected device
     * @throws MidiUnavailableException If the device cannot be accessed
     */
    public String startListening(String deviceName) throws MidiUnavailableException {
        // Close any previously open device with this name
        stopListening(deviceName);
        
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice selectedDevice = null;
        MidiDevice.Info selectedInfo = null;
        
        // Find the device by name
        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(deviceName)) {
                selectedDevice = MidiSystem.getMidiDevice(info);
                selectedInfo = info;
                break;
            }
        }
        
        if (selectedDevice == null) {
            throw new IllegalArgumentException("MIDI device not found: " + deviceName);
        }
        
        // Open the device and set up a receiver
        selectedDevice.open();
        Transmitter transmitter = selectedDevice.getTransmitter();
        transmitter.setReceiver(new MidiInputReceiver(deviceName));
        
        // Store the open device
        openDevices.put(deviceName, selectedDevice);
        
        logger.info("Now listening for MIDI input from: {}", deviceName);
        
        // Safely format the return string using the device name if selectedInfo is null
        return String.format("Connected to %s", 
            selectedInfo != null ? 
                selectedInfo.getName() + " - " + selectedInfo.getDescription() : 
                deviceName
        );
    }
    
    /**
     * Stops listening to a MIDI device
     * @param deviceName Name of the device to stop listening to
     */
    public void stopListening(String deviceName) {
        MidiDevice device = openDevices.remove(deviceName);
        if (device != null) {
            device.close();
            logger.info("Stopped listening to MIDI device: {}", deviceName);
        }
    }
    
    /**
     * Stops listening to all MIDI devices
     */
    public void stopAll() {
        for (Map.Entry<String, MidiDevice> entry : openDevices.entrySet()) {
            entry.getValue().close();
            logger.info("Stopped listening to MIDI device: {}", entry.getKey());
        }
        openDevices.clear();
    }
    
    /**
     * Receiver for MIDI messages
     */
    private class MidiInputReceiver implements Receiver {
        private final String deviceName;
        
        public MidiInputReceiver(String deviceName) {
            this.deviceName = deviceName;
        }
        
        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                
                if (sm.getCommand() == ShortMessage.NOTE_ON) {
                    int key = sm.getData1();
                    int velocity = sm.getData2();
                    
                    // Note: Some devices send NOTE_ON with velocity 0 instead of NOTE_OFF
                    if (velocity > 0) {
                        logger.info("Device: {} - Note On: {} velocity: {}", deviceName, key, velocity);
                        // TODO: Process note on event
                        processNoteOn(key, velocity, timeStamp);
                    } else {
                        logger.info("Device: {} - Note Off: {} (via velocity 0)", deviceName, key);
                        // TODO: Process note off event
                        processNoteOff(key, timeStamp);
                    }
                } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                    int key = sm.getData1();
                    logger.info("Device: {} - Note Off: {}", deviceName, key);
                    // TODO: Process note off event
                    processNoteOff(key, timeStamp);
                }
            }
        }
        
        @Override
        public void close() {
            // Nothing to do here
        }
    }
    
    /**
     * Process a note on event
     * @param key MIDI note number
     * @param velocity Velocity (0-127)
     * @param timeStamp Timestamp of the event
     */
    private void processNoteOn(int key, int velocity, long timeStamp) {
        // This is where you'll implement note processing logic
        // For now, we're just logging the event
    }
    
    /**
     * Process a note off event
     * @param key MIDI note number
     * @param timeStamp Timestamp of the event
     */
    private void processNoteOff(int key, long timeStamp) {
        // This is where you'll implement note processing logic
        // For now, we're just logging the event
    }
}