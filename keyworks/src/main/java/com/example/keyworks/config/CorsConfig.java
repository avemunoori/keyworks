package com.example.keyworks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow all origins for development
        config.addAllowedOrigin("*");
        // Allow common HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        // Allow all headers
        config.addAllowedHeader("*");
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Apply CORS configuration to all paths, including /output/**
        source.registerCorsConfiguration("/**", config);
        source.registerCorsConfiguration("/output/**", config);
        
        return new CorsFilter(source);
    }
}