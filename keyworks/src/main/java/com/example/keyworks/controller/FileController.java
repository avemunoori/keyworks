package com.example.keyworks.controller;

import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.service.SheetMusicService;
import com.example.keyworks.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final SheetMusicService sheetMusicService;
    private final FileService fileService;
    private final Path outputDir;

    public FileController(SheetMusicService sheetMusicService, FileService fileService) {
        this.sheetMusicService = sheetMusicService;
        this.fileService = fileService;
        
        // Get the output directory from the same property used in LilyPondService
        String outputDirPath = System.getProperty("lilypond.output.dir", "./output");
        this.outputDir = Paths.get(outputDirPath);
        
        // Create output directory if it doesn't exist
        this.outputDir.toFile().mkdirs();
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

    @GetMapping("/pdf/view/{id}")
    public ResponseEntity<Resource> viewPdf(@PathVariable Long id) {
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
                    "inline; filename=\"" + sheetMusic.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf\"")
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
    
    @GetMapping("/output/{filename:.+}")
    public ResponseEntity<Resource> serveOutputFile(@PathVariable String filename) {
        try {
            // Construct the path to the file in the output directory
            Path filePath = outputDir.resolve(filename);
            Resource resource = fileService.loadFileAsResource(filePath.toString());
            
            String contentType;
            if (filename.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.endsWith(".midi") || filename.endsWith(".mid")) {
                contentType = "audio/midi";
            } else if (filename.endsWith(".ly")) {
                contentType = "text/plain";
            } else {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}