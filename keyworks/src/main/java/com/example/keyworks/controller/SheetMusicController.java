package com.example.keyworks.controller;

import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.service.SheetMusicService;
import com.example.keyworks.service.UserService;
import com.example.keyworks.service.LilyPondService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}