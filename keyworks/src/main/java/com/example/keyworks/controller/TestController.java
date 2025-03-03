package com.example.keyworks.controller;

import com.example.keyworks.repository.UserRepository;
import com.example.keyworks.repository.SheetMusicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SheetMusicRepository sheetMusicRepository;

    @GetMapping("/db-status")
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Try to count users to test database connection
            long userCount = userRepository.count();
            long sheetMusicCount = sheetMusicRepository.count();
            
            response.put("status", "connected");
            response.put("userCount", userCount);
            response.put("sheetMusicCount", sheetMusicCount);
            response.put("message", "Database connection is working properly");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Database connection error: " + e.getMessage());
        }
        
        return response;
    }
}