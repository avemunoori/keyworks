package com.example.keyworks.service;

import com.example.keyworks.config.FileStorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MidiProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(MidiProcessingService.class);
    
    private final FileStorageConfig fileStorageConfig;
    private final LilyPondService lilyPondService;
    
    // MIDI device management
    private Receiver midiReceiver;
    private Transmitter midiTransmitter;
    private MidiDevice currentDevice;
    private boolean isRecording = false;
    private List<MidiEvent> recordedEvents = new CopyOnWriteArrayList<>();
    private long startTime;
    private String currentRecordingId;
    
    // Note tracking
    private final Map<Integer, Long> activeNotes = new ConcurrentHashMap<>();
    private final List<String> recordedNotes = new CopyOnWriteArrayList<>();
    
    // Note name mapping
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    
    public MidiProcessingService(FileStorageConfig fileStorageConfig, LilyPondService lilyPondService) {
        this.fileStorageConfig = fileStorageConfig;
        this.lilyPondService = lilyPondService;
        
        // Ensure output directory exists
        Path outputDir = fileStorageConfig.getOutputDirectoryPath();
        try {
            Files.createDirectories(outputDir);
            logger.info("Output directory created/verified at: {}", outputDir);
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", e.getMessage());
        }
    }
    
    /**
     * Gets a list of available MIDI devices
     * @return List of MIDI device info objects
     */
    public List<MidiDevice.Info> getMidiDevices() {
        logger.info("Fetching MIDI devices...");
        List<MidiDevice.Info> deviceInfos = new ArrayList<>();
        
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // Only include devices that can transmit MIDI data
                    if (device.getMaxTransmitters() != 0) {
                        deviceInfos.add(info);
                    }
                } catch (MidiUnavailableException e) {
                    logger.warn("MIDI device unavailable: {}", info.getName());
                }
            }
            logger.info("Found {} MIDI devices", deviceInfos.size());
        } catch (Exception e) {
            logger.error("Error getting MIDI devices: {}", e.getMessage());
        }
        
        return deviceInfos;
    }
    
    /**
     * Connects to a MIDI device
     * @param deviceInfo The MIDI device info
     * @return True if connection was successful
     */
    public boolean connectToDevice(MidiDevice.Info deviceInfo) {
        try {
            logger.info("Connecting to MIDI device: {}...", deviceInfo.getName());
            
            // Close any existing connection
            disconnectFromDevice();
            
            // Open the new device
            MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
            if (!device.isOpen()) {
                device.open();
            }
            
            // Set up the MIDI receiver
            midiReceiver = new MidiInputReceiver();
            
            // Get a transmitter from the device
            midiTransmitter = device.getTransmitter();
            midiTransmitter.setReceiver(midiReceiver);
            
            currentDevice = device;
            logger.info("Connected to MIDI device");
            return true;
        } catch (MidiUnavailableException e) {
            logger.error("Failed to connect to MIDI device: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnects from the current MIDI device
     */
    public void disconnectFromDevice() {
        try {
            if (isRecording) {
                stopRecording();
            }
            
            if (midiTransmitter != null) {
                midiTransmitter.close();
                midiTransmitter = null;
            }
            
            if (midiReceiver != null) {
                midiReceiver.close();
                midiReceiver = null;
            }
            
            if (currentDevice != null && currentDevice.isOpen()) {
                currentDevice.close();
                currentDevice = null;
            }
            
            logger.info("Disconnected from MIDI device");
        } catch (Exception e) {
            logger.error("Error disconnecting from MIDI device: {}", e.getMessage());
        }
    }
    
    /**
     * Starts recording MIDI input
     * @return The ID of the recording session
     */
    public String startRecording() {
        if (midiReceiver == null) {
            logger.warn("Cannot start recording: No MIDI device connected");
            return null;
        }
        
        logger.info("Starting recording...");
        isRecording = true;
        recordedEvents.clear();
        recordedNotes.clear();
        activeNotes.clear();
        startTime = System.currentTimeMillis();
        
        // Generate a unique ID for this recording
        currentRecordingId = UUID.randomUUID().toString();
        logger.info("Recording started with ID: {}", currentRecordingId);
        
        return currentRecordingId;
    }
    
    /**
     * Stops the current recording session
     * @return The list of recorded notes
     */
    public List<String> stopRecording() {
        if (!isRecording) {
            logger.warn("Cannot stop recording: Not currently recording");
            return Collections.emptyList();
        }
        
        logger.info("Stopping recording...");
        isRecording = false;
        
        // Calculate duration
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Recording stopped and processed");
        logger.info("Notes recorded: {}", recordedNotes.size());
        logger.info("Duration: {} seconds", duration / 1000.0);
        
        return new ArrayList<>(recordedNotes);
    }
    
    /**
     * Simulates playing a C major scale
     */
    public void simulateCMajorScale() {
        if (!isRecording) {
            logger.warn("Please start recording first");
            return;
        }
        
        logger.info("Simulating C major scale...");
        
        // C major scale notes
        String[] notes = {"C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5"};
        
        try {
            for (String note : notes) {
                // Convert note name to MIDI note number
                int noteNumber = noteNameToMidiNumber(note);
                
                // Simulate note on
                logger.info("Simulated Note On: {}", note);
                processNoteOn(noteNumber, 64);
                Thread.sleep(500);
                
                // Simulate note off
                logger.info("Simulated Note Off: {}", note);
                processNoteOff(noteNumber);
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            logger.error("Simulation interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Generates a PDF from the recorded notes
     * @return The path to the generated PDF file
     */
    public String generatePDFFromRecording() {
        if (recordedNotes.isEmpty()) {
            logger.warn("No notes recorded to generate PDF");
            return null;
        }
        
        return generatePDF(recordedNotes, currentRecordingId);
    }
    
    /**
     * Generates a PDF from a list of notes
     * @param notes The list of notes
     * @param id The ID to use for the generated files
     * @return The path to the generated PDF file
     */
    public String generatePDF(List<String> notes, String id) {
        try {
            // Generate LilyPond code from notes
            String lilyPondCode = generateLilyPondCode(notes);
            
            // Use LilyPondService to generate PDF
            Map<String, String> generatedFiles = lilyPondService.generateFiles(lilyPondCode, id);
            
            if (!generatedFiles.containsKey("pdf")) {
                logger.error("PDF generation failed: No PDF file returned");
                throw new RuntimeException("PDF generation failed");
            }
            
            String pdfPath = generatedFiles.get("pdf");
            
            // Get the full path to the PDF file
            Path fullPath = fileStorageConfig.resolveFilePath(pdfPath);
            
            // Check if the file exists
            boolean fileExists = Files.exists(fullPath);
            
            logger.info("PDF generated successfully! PDF Path: {} Output Directory: {} Access URL: /api/files/output/{}", 
                    fullPath, fileStorageConfig.getOutputDirectory(), pdfPath);
            
            logger.info("Checking file: {} File exists: {}", fullPath, fileExists);
            
            if (!fileExists) {
                logger.error("PDF file not found at expected path: {}", fullPath);
                
                // Try to find the file by listing the directory
                File outputDir = fileStorageConfig.getOutputDirectoryPath().toFile();
                File[] pdfFiles = outputDir.listFiles((dir, name) -> name.endsWith(".pdf") && name.contains(id));
                
                if (pdfFiles != null && pdfFiles.length > 0) {
                    logger.info("Found PDF file at alternative location: {}", pdfFiles[0].getPath());
                    return pdfFiles[0].getName();
                } else {
                    logger.error("No PDF file found with ID: {}", id);
                    throw new RuntimeException("PDF generation failed");
                }
            }
            
            return pdfPath;
        } catch (Exception e) {
            logger.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
    
    /**
     * Generates LilyPond code from a list of notes
     * @param notes The list of notes
     * @return The generated LilyPond code
     */
    private String generateLilyPondCode(List<String> notes) {
        StringBuilder lilyPondCode = new StringBuilder();
        
        // Add LilyPond header
        lilyPondCode.append("\\version \"2.20.0\"\n");
        lilyPondCode.append("\\score {\n");
        lilyPondCode.append("  \\new Staff {\n");
        lilyPondCode.append("    \\clef treble\n");
        lilyPondCode.append("    \\time 4/4\n");
        lilyPondCode.append("    \\tempo 4 = 120\n");
        
        // Add notes
        lilyPondCode.append("    ");
        for (String note : notes) {
            // Convert MIDI note name to LilyPond format
            String lilyNote = convertToLilyPondNote(note);
            lilyPondCode.append(lilyNote).append(" ");
        }
        lilyPondCode.append("\n");
        
        // Close LilyPond code
        lilyPondCode.append("  }\n");
        lilyPondCode.append("  \\layout { }\n");
        lilyPondCode.append("  \\midi { }\n");
        lilyPondCode.append("}\n");
        
        return lilyPondCode.toString();
    }
    
    /**
     * Converts a MIDI note name to LilyPond format
     * @param midiNote The MIDI note name (e.g., "C4")
     * @return The LilyPond note name
     */
    private String convertToLilyPondNote(String midiNote) {
        if (midiNote == null || midiNote.length() < 2) {
            return "c'";
        }
        
        // Extract note name and octave
        String noteName = midiNote.substring(0, midiNote.length() - 1).toLowerCase();
        int octave = Integer.parseInt(midiNote.substring(midiNote.length() - 1));
        
        // Convert sharp/flat notation
        if (noteName.contains("#")) {
            noteName = noteName.replace("#", "is");
        } else if (noteName.contains("b")) {
            noteName = noteName.replace("b", "es");
        }
        
        // Add octave markers
        StringBuilder lilyNote = new StringBuilder(noteName);
        
        // Middle C (C4) in LilyPond is c'
        if (octave == 4) {
            lilyNote.append("'");
        } else if (octave > 4) {
            for (int i = 0; i < octave - 4; i++) {
                lilyNote.append("'");
            }
        } else if (octave < 4) {
            for (int i = 0; i < 4 - octave; i++) {
                lilyNote.append(",");
            }
        }
        
        return lilyNote.toString();
    }
    
    /**
     * Converts a MIDI note number to a note name
     * @param noteNumber The MIDI note number
     * @return The note name (e.g., "C4")
     */
    private String midiNumberToNoteName(int noteNumber) {
        int octave = (noteNumber / 12) - 1;
        int note = noteNumber % 12;
        return NOTE_NAMES[note] + octave;
    }
    
    /**
     * Converts a note name to a MIDI note number
     * @param noteName The note name (e.g., "C4")
     * @return The MIDI note number
     */
    private int noteNameToMidiNumber(String noteName) {
        if (noteName == null || noteName.length() < 2) {
            return 60; // Middle C
        }
        
        String note = noteName.substring(0, noteName.length() - 1);
        int octave = Integer.parseInt(noteName.substring(noteName.length() - 1));
        
        int noteIndex = -1;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equalsIgnoreCase(note)) {
                noteIndex = i;
                break;
            }
        }
        
        if (noteIndex == -1) {
            return 60; // Default to middle C
        }
        
        return (octave + 1) * 12 + noteIndex;
    }
    
    /**
     * Processes a note on event
     * @param noteNumber The MIDI note number
     * @param velocity The velocity of the note
     */
    private void processNoteOn(int noteNumber, int velocity) {
        if (velocity > 0) {
            String noteName = midiNumberToNoteName(noteNumber);
            activeNotes.put(noteNumber, System.currentTimeMillis());
            recordedNotes.add(noteName);
            logger.info("Note On: {}", noteName);
        } else {
            processNoteOff(noteNumber);
        }
    }
    
    /**
     * Processes a note off event
     * @param noteNumber The MIDI note number
     */
    private void processNoteOff(int noteNumber) {
        if (activeNotes.containsKey(noteNumber)) {
            String noteName = midiNumberToNoteName(noteNumber);
            activeNotes.remove(noteNumber);
            logger.info("Note Off: {}", noteName);
        }
    }
    
    /**
     * Tests PDF generation with a simple scale
     * @param id The ID to use for the test
     * @return The path to the generated PDF file
     */
    public String testPdfGeneration(String id) {
        logger.info("Testing PDF generation with ID: {}...", id);
        
        // Create a simple C major scale
        List<String> notes = List.of("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5");
        
        String result = generatePDF(notes, id);
        logger.info("PDF test generation completed");
        return result;
    }
    
    /**
     * Gets the current recording ID
     * @return The current recording ID
     */
    public String getCurrentRecordingId() {
        return currentRecordingId;
    }
    
    /**
     * Gets the list of recorded notes
     * @return The list of recorded notes
     */
    public List<String> getRecordedNotes() {
        return new ArrayList<>(recordedNotes);
    }
    
    /**
     * Checks if recording is in progress
     * @return True if recording is in progress
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Inner class to handle MIDI input
     */
    private class MidiInputReceiver implements Receiver {
        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isRecording) {
                return;
            }
            
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                
                if (sm.getCommand() == ShortMessage.NOTE_ON) {
                    int noteNumber = sm.getData1();
                    int velocity = sm.getData2();
                    processNoteOn(noteNumber, velocity);
                } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                    int noteNumber = sm.getData1();
                    processNoteOff(noteNumber);
                }
            }
            
            // Store the event for later processing
            recordedEvents.add(new MidiEvent(message, timeStamp));
        }
        
        @Override
        public void close() {
            // Nothing to close
        }
    }
}