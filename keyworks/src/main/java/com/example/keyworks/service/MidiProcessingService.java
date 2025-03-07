package com.example.keyworks.service;

import com.example.keyworks.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MidiProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(MidiProcessingService.class);
    private final Queue<Note> noteQueue = new ConcurrentLinkedQueue<>();
    private final MidiDeviceService midiDeviceService;
    private Path outputDir;
    private boolean isRecording = false;
    private long recordingStartTime;

   
    public MidiProcessingService(MidiDeviceService midiDeviceService, 
                                @Value("${app.output.directory:./output}") String outputDirectory) {
        this.midiDeviceService = midiDeviceService;
        this.outputDir = Paths.get(outputDirectory).toAbsolutePath();
        
        // Create output directory if it doesn't exist
        try {
            Files.createDirectories(outputDir);
            logger.info("Output directory created/verified at: {}", outputDir);
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", e.getMessage());
        }
    }

    public void startRecording() {
        clearNotes();
        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        logger.info("Recording started at {}", recordingStartTime);
    }

    public void stopRecording() {
        isRecording = false;
        logger.info("Recording stopped. Captured {} notes", noteQueue.size());
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void processNoteOn(int note, int velocity, long timestamp) {
        Note newNote = new Note(note, velocity, timestamp, 0);
        noteQueue.add(newNote);
        logger.debug("Note ON added to queue: {}", newNote);
    }

    public void processNoteOff(int note, long timestamp) {
        // Find the matching note-on event
        for (Note queuedNote : noteQueue) {
            if (queuedNote.getNoteNumber() == note && queuedNote.getDuration() == 0) {
                queuedNote.setDuration(timestamp - queuedNote.getTimestamp());
                logger.debug("Note OFF processed: {}", queuedNote);
                break;
            }
        }
    }

    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>(noteQueue);
        return notes;
    }

    public void clearNotes() {
        noteQueue.clear();
        logger.info("Note queue cleared");
    }

    // Original method for backward compatibility
    public String generateLilyPondFile() {
        return generateLilyPondFile(null);
    }

    // New method with custom filename support
    public String generateLilyPondFile(String customFilename) {
        if (noteQueue.isEmpty()) {
            logger.warn("No notes to process");
            return null;
        }

        // Use custom filename if provided, otherwise use timestamp
        String filename;
        if (customFilename != null && !customFilename.isEmpty()) {
            filename = customFilename + ".ly";
        } else {
            // Create a timestamp for the filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            filename = "keyworks_" + timestamp + ".ly";
        }
        
        Path filePath = outputDir.resolve(filename);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            // Write LilyPond header
            writer.write("\\version \"2.20.0\"\n");
            writer.write("\\score {\n");
            writer.write("  \\new Staff {\n");
            writer.write("    \\clef treble\n");
            writer.write("    \\time 4/4\n");
            writer.write("    \\tempo 4 = 120\n");
            writer.write("    \\relative c' {\n");

            // Sort notes by timestamp
            List<Note> sortedNotes = new ArrayList<>(noteQueue);
            sortedNotes.sort(Comparator.comparing(Note::getTimestamp));

            // Convert MIDI notes to LilyPond notation
            for (Note note : sortedNotes) {
                if (note.getDuration() > 0) {  // Only process notes with duration
                    String lilyNote = convertMidiNoteToLilyPond(note);
                    writer.write("      " + lilyNote + "\n");
                }
            }

            // Close LilyPond score
            writer.write("    }\n");
            writer.write("  }\n");
            writer.write("  \\layout { }\n");
            writer.write("  \\midi { }\n");
            writer.write("}\n");

            logger.info("LilyPond file generated: {}", filePath);
            
            // Compile to PDF
            String lilypondFilePath = filePath.toString();
            boolean compiled = compileLilyPondToPdf(lilypondFilePath);
            
            if (compiled) {
                // Return the base filename without extension
                return filename.substring(0, filename.lastIndexOf('.'));
            } else {
                logger.error("Failed to compile LilyPond file to PDF");
                return null;
            }
            
        } catch (IOException e) {
            logger.error("Error generating LilyPond file: {}", e.getMessage());
            return null;
        }
    }

    private String convertMidiNoteToLilyPond(Note note) {
        // MIDI note numbers: C4 (middle C) = 60
        String[] noteNames = {"c", "cis", "d", "dis", "e", "f", "fis", "g", "gis", "a", "ais", "b"};
        int noteNumber = note.getNoteNumber();
        
        // Calculate octave and note name
        int octave = (noteNumber / 12) - 1;
        String noteName = noteNames[noteNumber % 12];
        
        // Calculate duration (simplified)
        // This is a basic approximation - you might need more sophisticated logic
        long durationMs = note.getDuration();
        String durationStr = "4";  // Default to quarter note
        
        if (durationMs < 200) {
            durationStr = "16";  // Sixteenth note
        } else if (durationMs < 400) {
            durationStr = "8";   // Eighth note
        } else if (durationMs < 800) {
            durationStr = "4";   // Quarter note
        } else if (durationMs < 1600) {
            durationStr = "2";   // Half note
        } else {
            durationStr = "1";   // Whole note
        }
        
        // Build LilyPond notation
        StringBuilder lilyNote = new StringBuilder(noteName);
        
        // Add octave markers
        int relativeToMiddleC = octave - 4;  // Middle C is C4
        if (relativeToMiddleC > 0) {
            lilyNote.append("'".repeat(relativeToMiddleC));
        } else if (relativeToMiddleC < 0) {
            lilyNote.append(",".repeat(Math.abs(relativeToMiddleC)));
        }
        
        // Add duration
        lilyNote.append(durationStr);
        
        return lilyNote.toString();
    }

    private boolean compileLilyPondToPdf(String lilypondFilePath) {
        logger.info("Compiling LilyPond file to PDF: {}", lilypondFilePath);
        
        try {
            // Get just the filename from the path
            Path lilypondFile = Paths.get(lilypondFilePath);
            String filename = lilypondFile.getFileName().toString();
            
            // Create a process to run LilyPond
            ProcessBuilder processBuilder = new ProcessBuilder(
                "lilypond", 
                "--output=" + outputDir.toAbsolutePath(), 
                filename  // Just use the filename since we're already in the output directory
            );
            
            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);
            
            // Set working directory to output directory
            processBuilder.directory(outputDir.toFile());
            
            // Start the process
            Process process = processBuilder.start();
            
            // Capture and log the output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("LilyPond: {}", line);
                }
            }
            
            // Wait for the process to complete
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("LilyPond compilation successful");
                return true;
            } else {
                logger.error("LilyPond compilation failed with exit code: {}", exitCode);
                return false;
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error compiling LilyPond file: {}", e.getMessage());
            return false;
        }
    }

    public String getOutputDirectory() {
        return outputDir.toString();
    }

    public void exportMidiFile(String filename) {
        if (noteQueue.isEmpty()) {
            logger.warn("No notes to export to MIDI");
            return;
        }

        try {
            // Create a sequence with PPQ division type and resolution of 480 ticks per beat
            Sequence sequence = new Sequence(Sequence.PPQ, 480);
            
            // Create a track
            Track track = sequence.createTrack();
            
            // Add a program change message to set the instrument (piano = 0)
            ShortMessage programChange = new ShortMessage();
            programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);
            track.add(new MidiEvent(programChange, 0));
            
            // Sort notes by timestamp
            List<Note> sortedNotes = new ArrayList<>(noteQueue);
            sortedNotes.sort(Comparator.comparing(Note::getTimestamp));
            
            // Calculate the first timestamp to normalize all times
            long firstTimestamp = sortedNotes.isEmpty() ? 0 : sortedNotes.get(0).getTimestamp();
            
            // Add note events to the track
            for (Note note : sortedNotes) {
                if (note.getDuration() > 0) {
                    // Calculate tick positions
                    long startTick = (note.getTimestamp() - firstTimestamp) / 2; // Convert ms to ticks (simplified)
                    long endTick = startTick + (note.getDuration() / 2);
                    
                    // Note On
                    ShortMessage noteOn = new ShortMessage();
                    noteOn.setMessage(ShortMessage.NOTE_ON, 0, note.getNoteNumber(), note.getVelocity());
                    track.add(new MidiEvent(noteOn, startTick));
                    
                    // Note Off
                    ShortMessage noteOff = new ShortMessage();
                    noteOff.setMessage(ShortMessage.NOTE_OFF, 0, note.getNoteNumber(), 0);
                    track.add(new MidiEvent(noteOff, endTick));
                }
            }
            
            // Write the MIDI sequence to a file
            Path midiFilePath = outputDir.resolve(filename + ".mid");
            MidiSystem.write(sequence, 1, midiFilePath.toFile());
            
            logger.info("MIDI file exported to: {}", midiFilePath);
            
        } catch (Exception e) {
            logger.error("Error exporting MIDI file: {}", e.getMessage());
        }
    }

    public void handleMidiMessage(MidiMessage message, long timestamp) {
        if (!isRecording) {
            return;
        }
        
        // Only process ShortMessage types (which include note on/off)
        if (message instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) message;
            int command = sm.getCommand();
            int note = sm.getData1();
            int velocity = sm.getData2();
            
            // Calculate relative timestamp
            long relativeTime = timestamp - recordingStartTime;
            
            if (command == ShortMessage.NOTE_ON && velocity > 0) {
                processNoteOn(note, velocity, relativeTime);
            } else if (command == ShortMessage.NOTE_OFF || (command == ShortMessage.NOTE_ON && velocity == 0)) {
                processNoteOff(note, relativeTime);
            }
        }
    }

    // Original method for backward compatibility
    public String generateSheetMusic() {
        return generateSheetMusic(null);
    }

    // New method with custom filename support
    public String generateSheetMusic(String customFilename) {
        String baseFilename = generateLilyPondFile(customFilename);
        if (baseFilename != null) {
            exportMidiFile(baseFilename);
            return baseFilename;
        }
        return null;
    }
    
    // Method to get the path to a PDF file
    public Path getPdfPath(String baseFilename) {
        return outputDir.resolve(baseFilename + ".pdf");
    }
}