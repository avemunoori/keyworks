package com.example.keyworks.service;

import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.model.User;
import com.example.keyworks.repository.SheetMusicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class SheetMusicService {

    private final SheetMusicRepository sheetMusicRepository;
    private final LilyPondService lilyPondService;

    
    public SheetMusicService(SheetMusicRepository sheetMusicRepository, LilyPondService lilyPondService) {
        this.sheetMusicRepository = sheetMusicRepository;
        this.lilyPondService = lilyPondService;
    }

    public List<SheetMusic> findAllSheetMusic() {
        return sheetMusicRepository.findAll();
    }

    public Optional<SheetMusic> findSheetMusicById(Long id) {
        return sheetMusicRepository.findById(id);
    }

    public List<SheetMusic> findSheetMusicByUser(User user) {
        return sheetMusicRepository.findByUser(user);
    }

    public List<SheetMusic> findSheetMusicByUserOrderByCreatedAtDesc(User user) {
        return sheetMusicRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<SheetMusic> findSheetMusicByTitle(String title) {
        return sheetMusicRepository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional
    public SheetMusic saveSheetMusic(SheetMusic sheetMusic) {
        return sheetMusicRepository.save(sheetMusic);
    }

    @Transactional
    public SheetMusic generateSheetMusic(SheetMusic sheetMusic) throws IOException, InterruptedException {
        // Generate PDF and MIDI files
        SheetMusic updatedSheetMusic = lilyPondService.generateFiles(sheetMusic);
        
        // Save the updated SheetMusic with file paths
        return sheetMusicRepository.save(updatedSheetMusic);
    }

    @Transactional
    public void deleteSheetMusic(Long id) {
        sheetMusicRepository.findById(id).ifPresent(sheetMusic -> {
            // Delete generated files
            lilyPondService.deleteFiles(sheetMusic);
            
            // Delete the database record
            sheetMusicRepository.deleteById(id);
        });
    }
}