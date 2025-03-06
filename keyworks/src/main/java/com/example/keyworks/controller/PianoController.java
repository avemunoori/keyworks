package com.example.keyworks.controller;

import com.example.keyworks.model.User;
import com.example.keyworks.model.SheetMusic;
import com.example.keyworks.service.MidiDeviceService;
import com.example.keyworks.service.SheetMusicService;
import com.example.keyworks.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class PianoController {
    
    @Autowired
    private MidiDeviceService midiDeviceService;
    
    @Autowired
    private SheetMusicService sheetMusicService;
    
    @Autowired
    private UserService userService;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${app.recording.max-time:300}")
    private Integer maxRecordingTime;
    
    @Value("${app.recording.auto-save:false}")
    private Boolean autoSave;
    
    @GetMapping("/")
    public String index() {
        return "redirect:/piano";
    }
    
    @GetMapping("/piano")
    public String piano(Model model, Authentication authentication) {
        // Add MIDI devices
        model.addAttribute("midiDevices", midiDeviceService.listMidiDevices());
        
        // Add app configuration
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("maxRecordingTime", maxRecordingTime);
        model.addAttribute("autoSave", autoSave);
        
        // Add user data if authenticated
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            // Get user by username from the authentication object
            String username = authentication.getName();
            Optional<User> userOptional = userService.findUserByUsername(username);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                model.addAttribute("currentUserId", user.getId());
                
                // Get user recordings
                List<SheetMusic> userRecordings = sheetMusicService.findSheetMusicByUser(user);
                model.addAttribute("userRecordings", userRecordings);
            }
        }
        
        return "piano";
    }
}