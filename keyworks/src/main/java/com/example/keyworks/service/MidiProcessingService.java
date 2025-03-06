package com.example.keyworks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MidiProcessingService {
  
  private static final Logger logger = LoggerFactory.getLogger(MidiProcessingService.class);
  private final Path outputDir;
  
  public MidiProcessingService() {
      // Get the output directory from system property or use default
      String outputDirPath = System.getProperty("lilypond.output.dir", "./output");
      this.outputDir = Paths.get(outputDirPath);
      
      // Create output directory if it doesn't exist
      outputDir.toFile().mkdirs();
      logger.info("MidiProcessingService initialized with output directory: {}", outputDir.toAbsolutePath());
  }
  
  /**
   * Process MIDI notes and generate LilyPond notation and PDF
   * @param recordingId The unique ID for this recording
   * @param notes The list of MIDI notes
   * @return true if processing was successful
   */
  public boolean processMidiToLilyPond(String recordingId, List<String> notes) {
      try {
          logger.info("Processing MIDI notes for recording ID: {}", recordingId);
          
          // Generate LilyPond file
          String lilypondContent = generateLilyPondNotation(notes);
          Path lilypondPath = outputDir.resolve(recordingId + ".ly");
          Files.write(lilypondPath, lilypondContent.getBytes());
          logger.info("LilyPond file generated at: {}", lilypondPath);
          
          // Compile LilyPond to PDF
          boolean pdfGenerated = compileLilyPondToPdf(lilypondPath.toString());
          
          if (pdfGenerated) {
              logger.info("PDF successfully generated for recording ID: {}", recordingId);
              
              // Generate MIDI file
              generateMidiFile(recordingId, notes);
              logger.info("MIDI file generated for recording ID: {}", recordingId);
              
              return true;
          } else {
              logger.error("Failed to generate PDF for recording ID: {}", recordingId);
              return false;
          }
      } catch (IOException e) {
          logger.error("Error processing MIDI to LilyPond: {}", e.getMessage(), e);
          return false;
      }
  }
  
  /**
   * Generate LilyPond notation from MIDI notes
   * @param notes The list of MIDI notes
   * @return LilyPond notation as a string
   */
  private String generateLilyPondNotation(List<String> notes) {
      logger.debug("Generating LilyPond notation for {} notes", notes.size());
      
      // This is a simplified example - in a real app, you'd have more complex logic
      StringBuilder sb = new StringBuilder();
      sb.append("\\version \"2.20.0\"\n");
      sb.append("\\score {\n");
      sb.append("  \\new Staff {\n");
      sb.append("    \\clef treble\n");
      sb.append("    \\time 4/4\n");
      sb.append("    \\tempo 4 = 120\n");
      sb.append("    ");
      
      // Convert notes to LilyPond format
      for (String note : notes) {
          // Simple conversion - would be more complex in reality
          sb.append(convertToLilyPondNote(note)).append(" ");
      }
      
      sb.append("\n  }\n");
      sb.append("  \\layout { }\n");
      sb.append("  \\midi { }\n");
      sb.append("}\n");
      
      return sb.toString();
  }
  
  /**
   * Convert a MIDI note to LilyPond notation
   * @param note The MIDI note (e.g., "C4", "D#3")
   * @return The LilyPond notation for the note
   */
  private String convertToLilyPondNote(String note) {
      // This is a simplified conversion - in a real app, you'd handle more cases
      if (note == null || note.isEmpty()) {
          return "r"; // Rest if no note
      }
      
      // Extract the note name and octave
      String noteName = note.substring(0, 1).toLowerCase();
      String accidental = note.length() > 2 && (note.charAt(1) == '#' || note.charAt(1) == 'b') 
          ? (note.charAt(1) == '#' ? "is" : "es") 
          : "";
      
      int octave = note.length() > 1 
          ? Integer.parseInt(note.substring(note.length() - 1)) 
          : 4; // Default to octave 4
      
      // Convert to LilyPond notation
      // In LilyPond, middle C (C4) is c', C5 is c'', C3 is c, etc.
      StringBuilder lilyNote = new StringBuilder(noteName + accidental);
      
      if (octave > 4) {
          // Add apostrophes for octaves above middle C
          for (int i = 0; i < octave - 4; i++) {
              lilyNote.append("'");
          }
      } else if (octave < 4) {
          // Add commas for octaves below middle C
          for (int i = 0; i < 4 - octave; i++) {
              lilyNote.append(",");
          }
      }
      
      return lilyNote.toString();
  }
  
  /**
   * Compile LilyPond file to PDF using the LilyPond executable
   * @param lilypondFilePath Path to the LilyPond file
   * @return true if compilation was successful
   */
  private boolean compileLilyPondToPdf(String lilypondFilePath) {
      logger.info("Compiling LilyPond file to PDF: {}", lilypondFilePath);
      
      try {
          // Create a process to run LilyPond
          ProcessBuilder processBuilder = new ProcessBuilder(
              "lilypond", 
              "--output=" + outputDir.toAbsolutePath(), 
              lilypondFilePath
          );
          
          // Redirect error stream to output stream
          processBuilder.redirectErrorStream(true);
          
          // Set working directory to the output directory
          processBuilder.directory(outputDir.toFile());
          
          // Start the process
          Process process = processBuilder.start();
          
          // Read the output
          List<String> output = new ArrayList<>();
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
              String line;
              while ((line = reader.readLine()) != null) {
                  output.add(line);
                  logger.debug("LilyPond: {}", line);
              }
          }
          
          // Wait for the process to complete
          boolean completed = process.waitFor(30, TimeUnit.SECONDS);
          
          if (!completed) {
              logger.error("LilyPond process timed out after 30 seconds");
              process.destroyForcibly();
              return false;
          }
          
          // Check the exit code
          int exitCode = process.exitValue();
          
          if (exitCode == 0) {
              logger.info("LilyPond compilation successful");
              
              // Verify that the PDF file exists
              String pdfPath = lilypondFilePath.replace(".ly", ".pdf");
              File pdfFile = new File(pdfPath);
              
              if (pdfFile.exists() && pdfFile.length() > 0) {
                  logger.info("PDF file generated: {}", pdfPath);
                  return true;
              } else {
                  logger.error("PDF file not found or empty: {}", pdfPath);
                  return false;
              }
          } else {
              logger.error("LilyPond compilation failed with exit code: {}", exitCode);
              logger.error("LilyPond output: {}", String.join("\n", output));
              return false;
          }
      } catch (IOException | InterruptedException e) {
          logger.error("Error executing LilyPond: {}", e.getMessage(), e);
          return false;
      }
  }
  
  /**
   * Generate MIDI file from notes
   * @param recordingId The unique ID for this recording
   * @param notes The list of MIDI notes
   * @throws IOException If file operations fail
   */
  private void generateMidiFile(String recordingId, List<String> notes) throws IOException {
      logger.debug("Generating MIDI file for recording ID: {}", recordingId);
      
      // LilyPond should have generated a MIDI file already if we included \midi { } in the score
      // Check if it exists
      Path midiPath = outputDir.resolve(recordingId + ".midi");
      
      if (Files.exists(midiPath)) {
          logger.info("MIDI file already generated by LilyPond: {}", midiPath);
          return;
      }
      
      // If LilyPond didn't generate a MIDI file, create a simple one
      logger.info("Creating basic MIDI file: {}", midiPath);
      
      // Create a simple MIDI file header (this is a minimal MIDI file structure)
      byte[] midiHeader = {
          0x4D, 0x54, 0x68, 0x64, // MThd header
          0x00, 0x00, 0x00, 0x06, // header length
          0x00, 0x01, // format type (1)
          0x00, 0x01, // number of tracks (1)
          0x00, 0x60, // division (96 ticks per quarter note)
          
          // Track chunk
          0x4D, 0x54, 0x72, 0x6B, // MTrk header
          0x00, 0x00, 0x00, 0x04, // track length (4 bytes)
          0x00, (byte) 0xFF, 0x2F, 0x00  // end of track
      };
      
      Files.write(midiPath, midiHeader);
      logger.info("Basic MIDI file created: {}", midiPath);
  }
  
  /**
   * Get the path to the PDF file for a recording
   * @param recordingId The unique ID for the recording
   * @return The Path object representing the PDF file
   */
  public Path getPdfPath(String recordingId) {
      return outputDir.resolve(recordingId + ".pdf");
  }
  
  /**
   * Get the path to the MIDI file for a recording
   * @param recordingId The unique ID for the recording
   * @return The Path object representing the MIDI file
   */
  public Path getMidiPath(String recordingId) {
      return outputDir.resolve(recordingId + ".midi");
  }
  
  /**
   * Get the path to the LilyPond file for a recording
   * @param recordingId The unique ID for the recording
   * @return The Path object representing the LilyPond file
   */
  public Path getLilypondPath(String recordingId) {
      return outputDir.resolve(recordingId + ".ly");
  }
  
  /**
   * Get the output directory where all files are stored
   * @return The Path object representing the output directory
   */
  public Path getOutputDirectory() {
      return outputDir;
  }
  
  /**
   * Check if a PDF file exists for a recording
   * @param recordingId The unique ID for the recording
   * @return true if the PDF file exists
   */
  public boolean pdfExists(String recordingId) {
      return Files.exists(getPdfPath(recordingId));
  }
  
  /**
   * Check if a MIDI file exists for a recording
   * @param recordingId The unique ID for the recording
   * @return true if the MIDI file exists
   */
  public boolean midiExists(String recordingId) {
      return Files.exists(getMidiPath(recordingId));
  }
  
  /**
   * Check if a LilyPond file exists for a recording
   * @param recordingId The unique ID for the recording
   * @return true if the LilyPond file exists
   */
  public boolean lilypondExists(String recordingId) {
      return Files.exists(getLilypondPath(recordingId));
  }
}