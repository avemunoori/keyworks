// Updated MidiDeviceService.java with methods needed by MidiController
package com.example.keyworks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MidiDeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(MidiDeviceService.class);
    private final Map<String, MidiDevice> openDevices = new ConcurrentHashMap<>();
    
    // Store active notes and their start times for duration calculation
    private final Map<Integer, Long> activeNotes = new HashMap<>();
    
    // Store velocities of active notes
    private final Map<Integer, Integer> activeNoteVelocities = new HashMap<>();
    
    // Store recorded notes for conversion to LilyPond
    private final List<MidiNote> recordedNotes = new ArrayList<>();
    
    // Map to store recordings by ID
    private final Map<String, Map<String, Object>> recordings = new HashMap<>();
    
    // Recording state
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private String currentRecordingId = null;
    
    /**
     * Get all available MIDI input devices
     * @return List of available input devices
     */
    public List<MidiDeviceWrapper> getAvailableInputDevices() {
        List<MidiDeviceWrapper> deviceList = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                // Only include devices that can transmit (input devices)
                if (device.getMaxTransmitters() != 0) {
                    deviceList.add(new MidiDeviceWrapper(device));
                }
            } catch (MidiUnavailableException e) {
                logger.error("Error accessing MIDI device: {}", info.getName(), e);
            }
        }
        
        return deviceList;
    }
    
    /**
     * Start recording from a MIDI device
     * @param deviceName Name of the device to record from
     * @return Recording ID
     * @throws MidiUnavailableException If the device cannot be accessed
     */
    public String startRecording(String deviceName) throws MidiUnavailableException {
        // Stop any ongoing recording
        if (isRecording) {
            stopRecording();
        }
        
        // Start listening to the device
        startListening(deviceName);
        
        // Clear previous recording data
        recordedNotes.clear();
        activeNotes.clear();
        activeNoteVelocities.clear();
        
        // Generate a unique recording ID
        currentRecordingId = UUID.randomUUID().toString();
        
        // Start recording
        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        logger.info("Started recording MIDI input with ID: {}", currentRecordingId);
        
        return currentRecordingId;
    }
    
    /**
     * Stop recording MIDI input
     * @return Recording data including LilyPond code
     */
    public Map<String, Object> stopRecording() {
        if (!isRecording) {
            throw new IllegalStateException("No active recording to stop");
        }
        
        isRecording = false;
        logger.info("Stopped recording. Captured {} notes", recordedNotes.size());
        
        // Convert recorded notes to LilyPond notation
        String lilyPondCode = convertToLilyPond();
        
        // Create recording data
        Map<String, Object> recordingData = new HashMap<>();
        recordingData.put("id", currentRecordingId);
        recordingData.put("noteCount", recordedNotes.size());
        recordingData.put("duration", System.currentTimeMillis() - recordingStartTime);
        recordingData.put("lilyPondCode", lilyPondCode);
        recordingData.put("notes", new ArrayList<>(recordedNotes)); // Clone the list
        
        // Store the recording
        recordings.put(currentRecordingId, recordingData);
        
        // Reset current recording ID
        String completedRecordingId = currentRecordingId;
        currentRecordingId = null;
        
        return recordings.get(completedRecordingId);
    }
    
    /**
     * Get recording data by ID
     * @param id Recording ID
     * @return Recording data
     */
    public Map<String, Object> getRecordingData(String id) {
        return recordings.get(id);
    }
    
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
     * Convert recorded MIDI notes to LilyPond notation
     * @return LilyPond code
     */
    private String convertToLilyPond() {
        if (recordedNotes.isEmpty()) {
            return "% No notes recorded\n\\version \"2.24.0\"\n{ c'4 }";
        }
        
        StringBuilder lilyPond = new StringBuilder();
        lilyPond.append("\\version \"2.24.0\"\n{\n  ");
        
        // Sort notes by start time
        recordedNotes.sort(Comparator.comparing(MidiNote::getStartTime));
        
        // Simple conversion - this can be enhanced for better rhythm detection
        for (MidiNote note : recordedNotes) {
            lilyPond.append(midiNoteToLilyPond(note)).append(" ");
        }
        
        lilyPond.append("\n}");
        return lilyPond.toString();
    }
    
    /**
     * Convert a MIDI note to LilyPond notation
     * @param note The MIDI note to convert
     * @return LilyPond notation for the note
     */
    private String midiNoteToLilyPond(MidiNote note) {
        // MIDI note 60 is middle C (c')
        String[] noteNames = {"c", "cis", "d", "dis", "e", "f", "fis", "g", "gis", "a", "ais", "b"};
        int octave = note.getKey() / 12 - 1;
        int noteIndex = note.getKey() % 12;
        
        String noteName = noteNames[noteIndex];
        
        // Add octave markers
        if (octave < 3) {
            noteName += ",".repeat(3 - octave);
        } else if (octave > 3) {
            noteName += "'".repeat(octave - 3);
        }
        
        // Determine note duration (simplified)
        String duration = determineDuration(note.getDuration());
        
        return noteName + duration;
    }
    
    /**
     * Determine the LilyPond duration based on the note duration in milliseconds
     * @param durationMs Duration in milliseconds
     * @return LilyPond duration string
     */
    private String determineDuration(long durationMs) {
        // Simple duration mapping - can be enhanced for better rhythm detection
        if (durationMs < 200) return "16";      // Sixteenth note
        else if (durationMs < 400) return "8";  // Eighth note
        else if (durationMs < 800) return "4";  // Quarter note
        else if (durationMs < 1600) return "2"; // Half note
        else return "1";                        // Whole note
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
                        processNoteOn(key, velocity, System.currentTimeMillis());
                    } else {
                        logger.info("Device: {} - Note Off: {} (via velocity 0)", deviceName, key);
                        processNoteOff(key, System.currentTimeMillis());
                    }
                } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                    int key = sm.getData1();
                    logger.info("Device: {} - Note Off: {}", deviceName, key);
                    processNoteOff(key, System.currentTimeMillis());
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
        if (isRecording) {
            activeNotes.put(key, timeStamp);
            activeNoteVelocities.put(key, velocity); // Store the velocity
        }
    }
    
    /**
     * Process a note off event
     * @param key MIDI note number
     * @param timeStamp Timestamp of the event
     */
    private void processNoteOff(int key, long timeStamp) {
        if (isRecording && activeNotes.containsKey(key)) {
            long startTime = activeNotes.remove(key);
            int velocity = activeNoteVelocities.remove(key); // Get the stored velocity
            long duration = timeStamp - startTime;
            
            MidiNote note = new MidiNote(key, velocity, startTime - recordingStartTime, duration);
            recordedNotes.add(note);
            
            logger.info("Recorded note: {} (duration: {} ms)", key, duration);
        }
    }
    
    /**
     * Class to represent a MIDI note with timing information
     */
    public static class MidiNote {
        private final int key;
        private final int velocity;
        private final long startTime;
        private final long duration;
        
        public MidiNote(int key, int velocity, long startTime, long duration) {
            this.key = key;
            this.velocity = velocity;
            this.startTime = startTime;
            this.duration = duration;
        }
        
        public int getKey() {
            return key;
        }
        
        public int getVelocity() {
            return velocity;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getDuration() {
            return duration;
        }
    }
    
    /**
     * Wrapper class for MidiDevice to provide access to device info
     */
    public static class MidiDeviceWrapper {
        private final MidiDevice device;
        
        public MidiDeviceWrapper(MidiDevice device) {
            this.device = device;
        }
        
        public MidiDevice.Info getDeviceInfo() {
            return device.getDeviceInfo();
        }
        
        public MidiDevice getDevice() {
            return device;
        }
    }
}