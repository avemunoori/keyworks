package com.example.keyworks.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for Spring MVC web resources.
 * This class configures resource handlers for serving static files,
 * including PDF files from the output directory.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
  
    @Override
    public void addResourceHandlers(@SuppressWarnings("null") ResourceHandlerRegistry registry) {
        // Create output directory if it doesn't exist
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            System.out.println("Created output directory at: " + outputDir.getAbsolutePath() + ", success: " + created);
        } else {
            System.out.println("Output directory exists at: " + outputDir.getAbsolutePath());
        }
        
        // Get the absolute path to the output directory
        Path outputPath = Paths.get("output").toAbsolutePath();
        
        // Format the path for use with Spring's resource handler
        // Ensure it has the proper prefix and ends with a separator
        String formattedPath = "file:" + outputPath.toString().replace("\\", "/");
        if (!formattedPath.endsWith("/")) {
            formattedPath += "/";
        }
        
        System.out.println("Configuring resource handler: /output/** -> " + formattedPath);
        
        // Register the resource handler for the output directory
        registry.addResourceHandler("/output/**")
               .addResourceLocations(formattedPath)
               .setCachePeriod(0); // Disable caching for development
        
        // Add handlers for other static resources if needed
        registry.addResourceHandler("/static/**")
               .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/web-piano/**")
               .addResourceLocations("classpath:/static/web-piano/");
    }
}