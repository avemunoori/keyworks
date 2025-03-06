package com.example.keyworks.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        
        if (error != null) {
            model.addAttribute("errorMsg", "Invalid username or password");
        }
        
        if (logout != null) {
            model.addAttribute("logoutMsg", "You have been logged out successfully");
        }
        
        return "login";
    }
    
    // Removed the home() method to fix the ambiguous mapping
    // If you need this functionality, consider adding a different path
    // or consolidating with PianoController
}