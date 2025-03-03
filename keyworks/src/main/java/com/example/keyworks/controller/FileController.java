package com.example.keyworks.controller;

import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.service.SheetMusicService;
import com.example.keyworks.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final SheetMusicService sheetMusicService;
    private final FileService fileService;

    public FileController(SheetMusicService sheetMusicService, FileService fileService) {
        this.sheetMusicService = sheetMusicService;
        this.fileService = fileService;
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        Optional<SheetMusic> sheetMusicOpt = sheetMusicService.findSheetMusicById(id);
        
        if (sheetMusicOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        SheetMusic sheetMusic = sheetMusicOpt.get();
        if (sheetMusic.getPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = fileService.loadFileAsResource(sheetMusic.getPdfPath());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + sheetMusic.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf\"")
                .body(resource);
    }

    @GetMapping("/midi/{id}")
    public ResponseEntity<Resource> downloadMidi(@PathVariable Long id) {
        Optional<SheetMusic> sheetMusicOpt = sheetMusicService.findSheetMusicById(id);
        
        if (sheetMusicOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        SheetMusic sheetMusic = sheetMusicOpt.get();
        if (sheetMusic.getMidiPath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = fileService.loadFileAsResource(sheetMusic.getMidiPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/midi"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + sheetMusic.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".midi\"")
                .body(resource);
    }
}