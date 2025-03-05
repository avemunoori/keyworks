package com.example.keyworks.controller;

import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.service.SheetMusicService;
import com.example.keyworks.service.UserService;
import com.example.keyworks.service.LilyPondService;
import com.example.keyworks.service.MidiDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sheet-music")
public class SheetMusicController {

    private final SheetMusicService sheetMusicService;
    private final UserService userService;
    private final LilyPondService lilyPondService;
    
    @Autowired
    private ApplicationContext applicationContext;

    public SheetMusicController(SheetMusicService sheetMusicService, UserService userService, LilyPondService lilyPondService) {
        this.sheetMusicService = sheetMusicService;
        this.userService = userService;
        this.lilyPondService = lilyPondService;
    }

    @GetMapping
    public ResponseEntity<List<SheetMusic>> getAllSheetMusic() {
        return ResponseEntity.ok(sheetMusicService.findAllSheetMusic());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SheetMusic> getSheetMusicById(@PathVariable Long id) {
        return sheetMusicService.findSheetMusicById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SheetMusic>> searchSheetMusic(@RequestParam String title) {
        return ResponseEntity.ok(sheetMusicService.findSheetMusicByTitle(title));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SheetMusic>> getSheetMusicByUser(@PathVariable Long userId) {
        return userService.findUserById(userId)
                .map(user -> ResponseEntity.ok(sheetMusicService.findSheetMusicByUser(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSheetMusic(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            String lilyPondCode = (String) payload.get("lilyPondCode");

            return userService.findUserById(userId)
                    .map(user -> {
                        SheetMusic sheetMusic = new SheetMusic(title, description, lilyPondCode, user);
                        SheetMusic savedSheetMusic = sheetMusicService.saveSheetMusic(sheetMusic);
                        return ResponseEntity.status(HttpStatus.CREATED).body(savedSheetMusic);
                    })
                    .orElseThrow();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSheetMusic(@PathVariable Long id, @RequestBody SheetMusic sheetMusic) {
        return sheetMusicService.findSheetMusicById(id)
                .map(existingSheetMusic -> {
                    sheetMusic.setId(id);
                    sheetMusic.setUser(existingSheetMusic.getUser());
                    return ResponseEntity.ok(sheetMusicService.saveSheetMusic(sheetMusic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSheetMusic(@PathVariable Long id) {
        return sheetMusicService.findSheetMusicById(id)
                .map(sheetMusic -> {
                    sheetMusicService.deleteSheetMusic(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate/{id}")
    public ResponseEntity<?> generateSheetMusicFiles(@PathVariable Long id) {
        try {
            return sheetMusicService.findSheetMusicById(id)
                    .map(sheetMusic -> {
                        try {
                            SheetMusic updatedSheetMusic = sheetMusicService.generateSheetMusic(sheetMusic);
                            return ResponseEntity.ok(updatedSheetMusic);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Map.of("error", "Failed to generate files: " + e.getMessage()));
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/with-files")
    public ResponseEntity<?> createSheetMusicWithFiles(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            String lilyPondCode = (String) payload.get("lilyPondCode");

            return userService.findUserById(userId)
                    .map(user -> {
                        try {
                            // Create and save the sheet music
                            SheetMusic sheetMusic = new SheetMusic(title, description, lilyPondCode, user);
                            SheetMusic savedSheetMusic = sheetMusicService.saveSheetMusic(sheetMusic);
                            
                            // Generate files
                            SheetMusic updatedSheetMusic = sheetMusicService.generateSheetMusic(savedSheetMusic);
                            return ResponseEntity.status(HttpStatus.CREATED).body(updatedSheetMusic);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Map.of("error", "Failed to generate files: " + e.getMessage()));
                        }
                    })
                    .orElse(ResponseEntity.badRequest().body(Map.of("error", "User not found")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/test-lilypond")
    public ResponseEntity<?> testLilyPond() {
        try {
            // Create a test SheetMusic object
            SheetMusic testSheetMusic = new SheetMusic();
            testSheetMusic.setTitle("Test Sheet Music");
            testSheetMusic.setDescription("A test sheet music for LilyPond integration");
            testSheetMusic.setLilyPondCode("\\version \"2.24.4\"\n{\n  c4 d e f\n  g2 g\n  a4 a a a\n  g1\n}");
            testSheetMusic.setCreatedAt(LocalDateTime.now());
            testSheetMusic.setUpdatedAt(LocalDateTime.now());
            
            // Note: We're not setting a user since this is just a test and we're not saving to the database
            // The user field is required for database persistence but not for the LilyPond file generation
    
            // Generate files using LilyPondService
            SheetMusic result = lilyPondService.generateFiles(testSheetMusic);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Files generated successfully",
                    "pdfPath", result.getPdfPath(),
                    "midiPath", result.getMidiPath() != null ? result.getMidiPath() : "No MIDI generated"
            ));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate files: " + e.getMessage()));
        }
    }
    
    /**
     * Convert a MIDI recording to PDF
     * @param recordingId ID of the recording to convert
     * @return PDF file as a downloadable resource
     */
    @GetMapping("/from-recording/{recordingId}")
    public ResponseEntity<?> generatePdfFromRecording(@PathVariable String recordingId,
                                                     @RequestParam(required = false) Long userId) {
        try {
            // Get the recording data from MidiDeviceService
            MidiDeviceService midiDeviceService = applicationContext.getBean(MidiDeviceService.class);
            Map<String, Object> recordingData = midiDeviceService.getRecordingData(recordingId);
            
            if (recordingData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Recording not found with ID: " + recordingId));
            }
            
            String lilyPondCode = (String) recordingData.get("lilyPondCode");
            
            if (lilyPondCode == null || lilyPondCode.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No LilyPond code found for recording: " + recordingId));
            }
            
            // Create a SheetMusic object
            SheetMusic sheetMusic = new SheetMusic();
            sheetMusic.setTitle("Recording " + recordingId);
            sheetMusic.setDescription("Generated from MIDI recording");
            sheetMusic.setLilyPondCode(lilyPondCode);
            sheetMusic.setCreatedAt(LocalDateTime.now());
            sheetMusic.setUpdatedAt(LocalDateTime.now());
            
            // Set user if provided
            if (userId != null) {
                userService.findUserById(userId).ifPresent(sheetMusic::setUser);
            }
            
            // Generate files using LilyPondService
            SheetMusic result = lilyPondService.generateFiles(sheetMusic);
            
            // Return the PDF file as a downloadable resource
            File pdfFile = new File(result.getPdfPath());
            if (!pdfFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "PDF file was not generated"));
            }
            
            FileSystemResource resource = new FileSystemResource(pdfFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"recording-" + recordingId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate PDF: " + e.getMessage()));
        }
    }
}