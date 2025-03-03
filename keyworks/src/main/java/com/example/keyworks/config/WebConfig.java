package com.example.keyworks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow requests from the frontend
        config.setAllowedOriginPatterns(List.of("http://localhost:3000"));
        
        // Allow all HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers
        config.setAllowedHeaders(List.of("*"));
        
        // Expose these headers to the client
        config.setExposedHeaders(List.of("Content-Disposition", "Content-Type"));
        
        // Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}