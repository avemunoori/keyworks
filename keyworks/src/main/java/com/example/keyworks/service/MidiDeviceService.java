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
                // Include all devices for now, we'll check capabilities later
                deviceList.add(new MidiDeviceWrapper(device));
            } catch (MidiUnavailableException e) {
                logger.error("Error accessing MIDI device: {}", info.getName(), e);
            }
        }
        
        return deviceList;
    }
    
    /**
     * Checks if a MIDI device can be used for recording
     * @param deviceName Name of the device to check
     * @return True if the device can be used for recording
     */
    public boolean canDeviceRecord(String deviceName) {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        
        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(deviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    boolean canTransmit = device.getMaxTransmitters() != 0;
                    logger.info("Device {} can transmit: {}", deviceName, canTransmit);
                    return canTransmit;
                } catch (MidiUnavailableException e) {
                    logger.error("Error checking MIDI device: {}", deviceName, e);
                    return false;
                }
            }
        }
        
        logger.warn("Device not found: {}", deviceName);
        return false;
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
        
        // Try the virtual MIDI input approach first
        try {
            listenToVirtualMidiInput(deviceName);
        } catch (Exception e) {
            logger.error("Failed to connect to virtual MIDI input: {}", e.getMessage());
            
            // Fall back to other methods
            try {
                listenDirectToDevice(deviceName);
            } catch (Exception ex) {
                logger.error("Failed to connect directly to device: {}", ex.getMessage());
                startListening(deviceName);
            }
        }
        
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
     * Listen to a MIDI IN port that corresponds to a virtual MIDI device
     * @param deviceName Name of the virtual device
     * @return Information about the connection status
     * @throws MidiUnavailableException If the device cannot be accessed
     */
    public String listenToVirtualMidiInput(String deviceName) throws MidiUnavailableException {
        // Close any previously open device with this name
        stopListening(deviceName);
        
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice inputDevice = null;
        
        // Log all available devices for debugging
        logger.info("Available MIDI devices:");
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                logger.info("Device: {} - {} (Max transmitters: {}, Max receivers: {})",
                    info.getName(), info.getDescription(),
                    device.getMaxTransmitters(), device.getMaxReceivers());
            } catch (MidiUnavailableException e) {
                logger.error("Error accessing MIDI device: {}", info.getName(), e);
            }
        }
        
        // First, try to find the exact matching input device by description
        String exactDescription = "Virtual Piano Bus 1";
        for (MidiDevice.Info info : infos) {
            if (info.getDescription().equals(exactDescription)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // We need a device that can transmit (has transmitters)
                    if (device.getMaxTransmitters() != 0) {
                        inputDevice = device;
                        logger.info("Found exact matching input device by description: {} - {}", 
                            info.getName(), info.getDescription());
                        break;
                    }
                } catch (MidiUnavailableException e) {
                    logger.error("Error accessing MIDI device: {}", info.getName(), e);
                }
            }
        }
        
        // If we didn't find an exact match by description, try by name
        if (inputDevice == null) {
            for (MidiDevice.Info info : infos) {
                if (info.getName().contains(deviceName)) {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(info);
                        // We need a device that can transmit
                        if (device.getMaxTransmitters() != 0) {
                            inputDevice = device;
                            logger.info("Found input device by name: {} - {}", 
                                info.getName(), info.getDescription());
                            break;
                        }
                    } catch (MidiUnavailableException e) {
                        logger.error("Error accessing MIDI device: {}", info.getName(), e);
                    }
                }
            }
        }
        
        // If we still don't have a device, try any input device
        if (inputDevice == null) {
            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    if (device.getMaxTransmitters() != 0 && 
                        !(device instanceof Sequencer) && 
                        !(device instanceof Synthesizer)) {
                        inputDevice = device;
                        logger.info("Found fallback input device: {} - {}", 
                            info.getName(), info.getDescription());
                        break;
                    }
                } catch (MidiUnavailableException e) {
                    logger.error("Error accessing MIDI device: {}", info.getName(), e);
                }
            }
        }
        
        if (inputDevice == null) {
            throw new IllegalArgumentException("No suitable MIDI input device found");
        }
        
        // Open the device if it's not already open
        if (!inputDevice.isOpen()) {
            inputDevice.open();
        }
        
        // Set up a transmitter and connect it to our receiver
        Transmitter transmitter = inputDevice.getTransmitter();
        MidiInputReceiver receiver = new MidiInputReceiver(inputDevice.getDeviceInfo().getName());
        transmitter.setReceiver(receiver);
        
        // Store the open device
        openDevices.put(deviceName, inputDevice);
        
        logger.info("Successfully connected to MIDI input device: {}", inputDevice.getDeviceInfo().getName());
        return "Connected to " + inputDevice.getDeviceInfo().getName();
    }
    
    /**
     * Direct method to listen to a specific MIDI device
     * @param deviceName Name of the device to listen to
     * @return Information about the connection status
     * @throws MidiUnavailableException If the device cannot be accessed
     */
    public String listenDirectToDevice(String deviceName) throws MidiUnavailableException {
        // Close any previously open device with this name
        stopListening(deviceName);
        
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice selectedDevice = null;
        
        // Find the device by name
        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(deviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // We need a device that can transmit (has transmitters)
                    if (device.getMaxTransmitters() != 0) {
                        selectedDevice = device;
                        logger.info("Found device: {} - {}", info.getName(), info.getDescription());
                        logger.info("Max transmitters: {}, Max receivers: {}", 
                            device.getMaxTransmitters(), device.getMaxReceivers());
                        break;
                    }
                } catch (MidiUnavailableException e) {
                    logger.error("Error accessing MIDI device: {}", info.getName(), e);
                }
            }
        }
        
        if (selectedDevice == null) {
            throw new IllegalArgumentException("MIDI device not found: " + deviceName);
        }
        
        // Open the device if it's not already open
        if (!selectedDevice.isOpen()) {
            selectedDevice.open();
        }
        
        // Create and attach our custom receiver
        Transmitter transmitter = selectedDevice.getTransmitter();
        MidiInputReceiver receiver = new MidiInputReceiver(deviceName);
        transmitter.setReceiver(receiver);
        
        // Store the open device
        openDevices.put(deviceName, selectedDevice);
        
        logger.info("Successfully connected to MIDI device: {}", deviceName);
        return "Connected to " + deviceName;
    }
    
    /**
     * Check if a device is a virtual MIDI device
     * @param deviceName Name of the device to check
     * @return True if the device is a virtual MIDI device
     */
    private boolean isVirtualMidiDevice(String deviceName) {
        // Check for common virtual MIDI device indicators
        return deviceName.contains("IAC") || 
               deviceName.contains("Bus") || 
               deviceName.contains("Virtual") ||
               deviceName.contains("LoopBe");
    }
    
    /**
     * Special method to connect to virtual MIDI devices
     * @param deviceName Name of the virtual device
     * @throws MidiUnavailableException If the device cannot be accessed
     */
    @SuppressWarnings("unused")
    private void startListeningToVirtualDevice(String deviceName) throws MidiUnavailableException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice selectedDevice = null;
        
        // Find the device by name
        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(deviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // For virtual devices, we need to check if they can transmit
                    if (device.getMaxTransmitters() != 0) {
                        selectedDevice = device;
                        break;
                    }
                } catch (MidiUnavailableException e) {
                    logger.error("Error accessing MIDI device: {}", info.getName(), e);
                }
            }
        }
        
        if (selectedDevice == null) {
            throw new IllegalArgumentException("Virtual MIDI device not found: " + deviceName);
        }
        
        // Open the device
        if (!selectedDevice.isOpen()) {
            selectedDevice.open();
        }
        
        // Set up a transmitter and connect it to our receiver
        Transmitter transmitter = selectedDevice.getTransmitter();
        transmitter.setReceiver(new MidiInputReceiver(deviceName));
        
        logger.info("Connected to virtual MIDI device: {}", deviceName);
        
        // Store the open device
        openDevices.put(deviceName, selectedDevice);
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
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // We need a device that can transmit
                    if (device.getMaxTransmitters() != 0) {
                        selectedDevice = device;
                        selectedInfo = info;
                        break;
                    }
                } catch (MidiUnavailableException e) {
                    logger.error("Error accessing MIDI device: {}", info.getName(), e);
                }
            }
        }
        
        if (selectedDevice == null) {
            throw new IllegalArgumentException("MIDI device not found: " + deviceName);
        }
        
        // Open the device and set up a receiver
        try {
            selectedDevice.open();
            
            // For virtual devices, we might need a different approach
            if (isVirtualMidiDevice(deviceName)) {
                // This is a placeholder for virtual device handling
                logger.info("Using virtual device mode for: {}", deviceName);
                
                // For some virtual devices, we might need to use a Sequencer
                if (selectedDevice instanceof Sequencer) {
                    Sequencer sequencer = (Sequencer) selectedDevice;
                    sequencer.addMetaEventListener(meta -> {
                        // Handle meta events if needed
                    });
                    
                    // You might need to connect the sequencer to a synthesizer
                    // sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
                }
            } else {
                // Standard approach for physical MIDI devices
                Transmitter transmitter = selectedDevice.getTransmitter();
                transmitter.setReceiver(new MidiInputReceiver(deviceName));
            }
            
            // Store the open device
            openDevices.put(deviceName, selectedDevice);
            
            logger.info("Now listening for MIDI input from: {}", deviceName);
            
            // Safely format the return string using the device name if selectedInfo is null
            return String.format("Connected to %s", 
                selectedInfo != null ? 
                    selectedInfo.getName() + " - " + selectedInfo.getDescription() : 
                    deviceName
            );
        } catch (MidiUnavailableException e) {
            logger.error("Failed to open MIDI device: {}", deviceName, e);
            
            // Special handling for "transmitter not available" error
            if (e.getMessage() != null && e.getMessage().contains("transmitter")) {
                logger.info("Device doesn't support transmitters. Trying alternative approach...");
                
                // For devices that don't have transmitters, we might need to use them as receivers
                // This is a placeholder - actual implementation depends on your specific device
                
                // Store the open device anyway
                openDevices.put(deviceName, selectedDevice);
                
                return String.format("Connected to %s (alternative mode)", deviceName);
            }
            
            throw e;
        }
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
     * Process a note on event
     * @param key MIDI note number
     * @param velocity Velocity (0-127)
     * @param timeStamp Timestamp of the event
     */
    public void processNoteOn(int key, int velocity, long timeStamp) {
        logger.info("processNoteOn called - Recording state: {}", isRecording);
        if (isRecording) {
            activeNotes.put(key, timeStamp);
            activeNoteVelocities.put(key, velocity);
            logger.info("Stored Note On: {} velocity: {} at time: {}", key, velocity, timeStamp);
        }
    }
    
    /**
     * Process a note off event
     * @param key MIDI note number
     * @param timeStamp Timestamp of the event
     */
    public void processNoteOff(int key, long timeStamp) {
        logger.info("processNoteOff called - Recording state: {}", isRecording);
        if (isRecording && activeNotes.containsKey(key)) {
            long startTime = activeNotes.remove(key);
            int velocity = activeNoteVelocities.remove(key);
            long duration = timeStamp - startTime;
            
            MidiNote note = new MidiNote(key, velocity, startTime - recordingStartTime, duration);
            recordedNotes.add(note);
            
            logger.info("Recorded note: {} velocity: {} duration: {} ms", key, velocity, duration);
        }
    }
    
    /**
     * Simulate MIDI input for testing
     * @param notes Array of MIDI note numbers to simulate
     * @return Recording ID
     */
    public String simulateMidiInput(int[] notes) {
        // Start a new recording
        currentRecordingId = UUID.randomUUID().toString();
        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        
        // Clear previous recording data
        recordedNotes.clear();
        activeNotes.clear();
        activeNoteVelocities.clear();
        
        // Simulate playing each note
        long currentTime = System.currentTimeMillis();
        for (int note : notes) {
            // Note on
            processNoteOn(note, 100, currentTime);
            currentTime += 500; // 500ms note duration
            
            // Note off
            processNoteOff(note, currentTime);
            currentTime += 100; // 100ms between notes
        }
        
        // Stop recording and return the data
        Map<String, Object> recordingData = new HashMap<>();
        recordingData.put("id", currentRecordingId);
        recordingData.put("noteCount", recordedNotes.size());
        recordingData.put("duration", currentTime - recordingStartTime);
        recordingData.put("lilyPondCode", convertToLilyPond());
        recordingData.put("notes", new ArrayList<>(recordedNotes));
        
        // Store the recording
        recordings.put(currentRecordingId, recordingData);
        
        // Reset recording state
        isRecording = false;
        String completedRecordingId = currentRecordingId;
        currentRecordingId = null;
        
        return completedRecordingId;
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
        lilyPond.append("\\version \"2.24.0\"\n");
        lilyPond.append("\\header {\n");
        lilyPond.append("  title = \"Recorded MIDI Performance\"\n");
        lilyPond.append("  composer = \"Generated by KeyWorks\"\n");
        lilyPond.append("  tagline = \"Created on " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + "\"\n");
        lilyPond.append("}\n\n");
        
        lilyPond.append("\\score {\n");
        lilyPond.append("  \\new Staff {\n");
        lilyPond.append("    \\clef treble\n");
        lilyPond.append("    \\time 4/4\n");
        lilyPond.append("    \\tempo 4 = 120\n\n");
        lilyPond.append("    ");
        
        // Sort notes by start time
        recordedNotes.sort(Comparator.comparing(MidiNote::getStartTime));
        
        // Simple conversion - this can be enhanced for better rhythm detection
        for (MidiNote note : recordedNotes) {
            lilyPond.append(midiNoteToLilyPond(note)).append(" ");
        }
        
        lilyPond.append("\n  }\n");
        lilyPond.append("  \\layout { }\n");
        lilyPond.append("  \\midi { }\n");
        lilyPond.append("}\n");
        
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
            logger.info("Created new MidiInputReceiver for device: {}", deviceName);
        }
        
        @Override
        public void send(MidiMessage message, long timeStamp) {
            // Log ALL incoming messages in hex format for debugging
            byte[] data = message.getMessage();
            StringBuilder hexString = new StringBuilder();
            for (byte b : data) {
                hexString.append(String.format("%02X ", b));
            }
            logger.info("RAW MIDI: {} - bytes: {} hex: {}", deviceName, data.length, hexString.toString());
            
            // Process ShortMessage (Note On/Off)
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                int command = sm.getCommand();
                int channel = sm.getChannel();
                int data1 = sm.getData1();
                int data2 = sm.getData2();
                
                logger.info("MIDI Command: {} (0x{}) Channel: {} Data1: {} Data2: {}", 
                    commandToString(command), String.format("%02X", command), 
                    channel, data1, data2);
                
                // Process Note On (including all channels)
                if (command == ShortMessage.NOTE_ON) {
                    int key = data1;
                    int velocity = data2;
                    
                    // Note: Some devices send NOTE_ON with velocity 0 instead of NOTE_OFF
                    if (velocity > 0) {
                        logger.info("Processing Note On - Device: {} - Note: {} velocity: {}", 
                            deviceName, key, velocity);
                        processNoteOn(key, velocity, System.currentTimeMillis());
                    } else {
                        logger.info("Processing Note Off (via velocity 0) - Device: {} - Note: {}", 
                            deviceName, key);
                        processNoteOff(key, System.currentTimeMillis());
                    }
                } 
                // Process Note Off
                else if (command == ShortMessage.NOTE_OFF) {
                    int key = data1;
                    logger.info("Processing Note Off - Device: {} - Note: {}", deviceName, key);
                    processNoteOff(key, System.currentTimeMillis());
                }
                // Log other MIDI commands for debugging
                else {
                    logger.info("Other MIDI command received: {}", commandToString(command));
                }
            }
            // Log other message types
            else {
                logger.info("Non-ShortMessage received: {}", message.getClass().getSimpleName());
            }
        }
        
        // Helper method to convert MIDI command to string
        private String commandToString(int command) {
            switch (command) {
                case ShortMessage.NOTE_ON: return "NOTE_ON";
                case ShortMessage.NOTE_OFF: return "NOTE_OFF";
                case ShortMessage.CONTROL_CHANGE: return "CONTROL_CHANGE";
                case ShortMessage.PROGRAM_CHANGE: return "PROGRAM_CHANGE";
                case ShortMessage.PITCH_BEND: return "PITCH_BEND";
                case ShortMessage.CHANNEL_PRESSURE: return "CHANNEL_PRESSURE";
                case ShortMessage.POLY_PRESSURE: return "POLY_PRESSURE";
                default: return "UNKNOWN(" + command + ")";
            }
        }
        
        @Override
        public void close() {
            logger.info("Closing MidiInputReceiver for device: {}", deviceName);
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