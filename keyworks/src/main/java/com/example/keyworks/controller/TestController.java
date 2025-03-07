package com.example.keyworks.controller;

import com.example.keyworks.config.FileStorageConfig;
import com.example.keyworks.service.FileService;
import com.example.keyworks.service.LilyPondService;
import com.example.keyworks.service.MidiProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/test")
public class TestController {
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    private final FileStorageConfig fileStorageConfig;
    private final FileService fileService;
    private final LilyPondService lilyPondService;
    private final MidiProcessingService midiProcessingService;
    
    public TestController(
            FileStorageConfig fileStorageConfig,
            FileService fileService,
            LilyPondService lilyPondService,
            MidiProcessingService midiProcessingService) {
        this.fileStorageConfig = fileStorageConfig;
        this.fileService = fileService;
        this.lilyPondService = lilyPondService;
        this.midiProcessingService = midiProcessingService;
        logger.info("TestController initialized");
    }
    
    /**
     * Tests the system configuration
     * @return System configuration information
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> testConfig() {
        Map<String, Object> response = new HashMap<>();
        
        // Get system properties
        Properties props = System.getProperties();
        Map<String, String> systemProps = new HashMap<>();
        systemProps.put("java.version", props.getProperty("java.version"));
        systemProps.put("java.home", props.getProperty("java.home"));
        systemProps.put("os.name", props.getProperty("os.name"));
        systemProps.put("os.version", props.getProperty("os.version"));
        systemProps.put("user.dir", props.getProperty("user.dir"));
        
        // Get application configuration
        Map<String, String> appConfig = new HashMap<>();
        appConfig.put("outputDirectory", fileStorageConfig.getOutputDirectory());
        
        // Check if output directory exists and is writable
        File outputDir = new File(fileStorageConfig.getOutputDirectory());
        boolean outputDirExists = outputDir.exists();
        boolean outputDirWritable = outputDir.canWrite();
        
        response.put("systemProperties", systemProps);
        response.put("applicationConfig", appConfig);
        response.put("outputDirectoryExists", outputDirExists);
        response.put("outputDirectoryWritable", outputDirWritable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Tests file creation in the output directory
     * @return Information about the created test file
     */
    @PostMapping("/file/create")
    public ResponseEntity<Map<String, Object>> testFileCreation() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Generate a unique test file name
            String testFileName = "test_file_" + UUID.randomUUID().toString() + ".txt";
            Path testFilePath = fileStorageConfig.resolveFilePath(testFileName);
            
            // Write test content to the file
            String testContent = "This is a test file created at " + new Date();
            Files.write(testFilePath, testContent.getBytes());
            
            // Check if the file was created successfully
            boolean fileExists = Files.exists(testFilePath);
            long fileSize = Files.size(testFilePath);
            
            response.put("success", fileExists);
            response.put("fileName", testFileName);
            response.put("filePath", testFilePath.toString());
            response.put("fileSize", fileSize);
            response.put("fileContent", testContent);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error creating test file: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tests file reading from the output directory
     * @param fileName The name of the file to read
     * @return The content of the file
     */
    @GetMapping("/file/read/{fileName}")
    public ResponseEntity<Map<String, Object>> testFileReading(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = fileStorageConfig.resolveFilePath(fileName);
            
            // Check if the file exists
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("error", "File not found: " + fileName);
                return ResponseEntity.status(404).body(response);
            }
            
            // Read the file content
            String fileContent = new String(Files.readAllBytes(filePath));
            long fileSize = Files.size(filePath);
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("filePath", filePath.toString());
            response.put("fileSize", fileSize);
            response.put("fileContent", fileContent);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error reading test file: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tests LilyPond PDF generation
     * @param id The ID to use for the generated files
     * @return Information about the generated PDF
     */
    @PostMapping("/lilypond/{id}")
    public ResponseEntity<Map<String, Object>> testLilyPond(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Simple LilyPond code for a C major scale
            String lilyPondCode = "\\version \"2.20.0\"\n" +
                    "\\score {\n" +
                    "  \\new Staff {\n" +
                    "    \\clef treble\n" +
                    "    \\time 4/4\n" +
                    "    \\tempo 4 = 120\n" +
                    "    c' d' e' f' g' a' b' c''\n" +
                    "  }\n" +
                    "  \\layout { }\n" +
                    "  \\midi { }\n" +
                    "}\n";
            
            // Generate PDF using LilyPondService
            Map<String, String> generatedFiles = lilyPondService.generateFiles(lilyPondCode, id);
            
            if (!generatedFiles.containsKey("pdf")) {
                response.put("success", false);
                response.put("error", "PDF generation failed");
                return ResponseEntity.status(500).body(response);
            }
            
            String pdfPath = generatedFiles.get("pdf");
            Path fullPdfPath = fileStorageConfig.resolveFilePath(pdfPath);
            
            // Check if the PDF file exists
            boolean pdfExists = Files.exists(fullPdfPath);
            long pdfSize = pdfExists ? Files.size(fullPdfPath) : 0;
            
            response.put("success", pdfExists);
            response.put("id", id);
            response.put("pdfPath", pdfPath);
            response.put("fullPdfPath", fullPdfPath.toString());
            response.put("pdfSize", pdfSize);
            response.put("pdfUrl", "/api/files/output/" + pdfPath);
            
            // Check if MIDI file was generated
            if (generatedFiles.containsKey("midi")) {
                String midiPath = generatedFiles.get("midi");
                Path fullMidiPath = fileStorageConfig.resolveFilePath(midiPath);
                boolean midiExists = Files.exists(fullMidiPath);
                long midiSize = midiExists ? Files.size(fullMidiPath) : 0;
                
                response.put("midiPath", midiPath);
                response.put("fullMidiPath", fullMidiPath.toString());
                response.put("midiExists", midiExists);
                response.put("midiSize", midiSize);
                response.put("midiUrl", "/api/files/output/" + midiPath);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing LilyPond: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tests MIDI processing with a simulated C major scale
     * @return Information about the processed MIDI data
     */
    @PostMapping("/midi/simulate")
    public ResponseEntity<Map<String, Object>> testMidiSimulation() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Start recording
            String recordingId = midiProcessingService.startRecording();
            if (recordingId == null) {
                response.put("success", false);
                response.put("error", "Failed to start recording");
                return ResponseEntity.status(500).body(response);
            }
            
            // Simulate C major scale
            midiProcessingService.simulateCMajorScale();
            
            // Stop recording
            List<String> notes = midiProcessingService.stopRecording();
            
            // Generate PDF
            String pdfPath = midiProcessingService.generatePDF(notes, recordingId);
            
            response.put("success", true);
            response.put("recordingId", recordingId);
            response.put("notes", notes);
            response.put("noteCount", notes.size());
            response.put("pdfPath", pdfPath);
            response.put("pdfUrl", "/api/files/output/" + pdfPath);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing MIDI simulation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tests file serving from the output directory
     * @param fileName The name of the file to serve
     * @return The file as a resource
     */
    @GetMapping("/file/serve/{fileName}")
    public ResponseEntity<Resource> testFileServing(@PathVariable String fileName) {
        try {
            // Load the file as a resource
            Resource resource = fileService.loadFileAsResource(fileName);
            
            // Check if the file exists
            if (!resource.exists()) {
                logger.error("File not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType;
            try {
                Path path = fileStorageConfig.resolveFilePath(fileName);
                contentType = Files.probeContentType(path);
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }
            
            if (contentType == null) {
                // Default content types based on extension
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".mid") || fileName.endsWith(".midi")) {
                    contentType = "audio/midi";
                } else if (fileName.endsWith(".txt")) {
                    contentType = "text/plain";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Lists all files in the output directory
     * @return A list of files in the output directory
     */
    @GetMapping("/files/list")
    public ResponseEntity<Map<String, Object>> listFiles() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path outputDir = fileStorageConfig.getOutputDirectoryPath();
            File outputDirFile = outputDir.toFile();
            
            if (!outputDirFile.exists() || !outputDirFile.isDirectory()) {
                response.put("success", false);
                response.put("error", "Output directory does not exist");
                return ResponseEntity.status(500).body(response);
            }
            
            File[] files = outputDirFile.listFiles();
            List<Map<String, Object>> fileList = new ArrayList<>();
            
            if (files != null) {
                for (File file : files) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("size", file.length());
                    fileInfo.put("isDirectory", file.isDirectory());
                    fileInfo.put("lastModified", new Date(file.lastModified()));
                    fileInfo.put("canRead", file.canRead());
                    fileInfo.put("canWrite", file.canWrite());
                    
                    // Try to determine content type
                    try {
                        String contentType = Files.probeContentType(file.toPath());
                        fileInfo.put("contentType", contentType);
                    } catch (IOException e) {
                        fileInfo.put("contentType", "unknown");
                    }
                    
                    fileList.add(fileInfo);
                }
            }
            
            response.put("success", true);
            response.put("outputDirectory", outputDir.toString());
            response.put("files", fileList);
            response.put("fileCount", fileList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error listing files: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tests path resolution
     * @param path The path to resolve
     * @return Information about the resolved path
     */
    @GetMapping("/path/resolve")
    public ResponseEntity<Map<String, Object>> testPathResolution(@RequestParam String path) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path resolvedPath = fileStorageConfig.resolveFilePath(path);
            
            response.put("success", true);
            response.put("originalPath", path);
            response.put("resolvedPath", resolvedPath.toString());
            response.put("exists", Files.exists(resolvedPath));
            response.put("isDirectory", Files.isDirectory(resolvedPath));
            response.put("isRegularFile", Files.isRegularFile(resolvedPath));
            response.put("isReadable", Files.isReadable(resolvedPath));
            response.put("isWritable", Files.isWritable(resolvedPath));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error resolving path: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Tests the entire PDF generation pipeline
     * @param id The ID to use for the test
     * @return Information about the generated files
     */
    @PostMapping("/pipeline/{id}")
    public ResponseEntity<Map<String, Object>> testPipeline(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Testing pipeline with ID: {}", id);
            
            // Step 1: Create a test file
            String testFileName = id + ".txt";
            Path testFilePath = fileStorageConfig.resolveFilePath(testFileName);
            String testContent = "Test file for pipeline test " + id;
            Files.write(testFilePath, testContent.getBytes());
            
            // Step 2: Generate LilyPond code
            String lilyPondCode = "\\version \"2.20.0\"\n" +
                    "\\score {\n" +
                    "  \\new Staff {\n" +
                    "    \\clef treble\n" +
                    "    \\time 4/4\n" +
                    "    \\tempo 4 = 120\n" +
                    "    c' d' e' f' g' a' b' c''\n" +
                    "  }\n" +
                    "  \\layout { }\n" +
                    "  \\midi { }\n" +
                    "}\n";
            
            // Step 3: Generate PDF and MIDI files
            Map<String, String> generatedFiles = lilyPondService.generateFiles(lilyPondCode, id);
            
            // Step 4: Check if files were generated
            String pdfPath = generatedFiles.getOrDefault("pdf", null);
            String midiPath = generatedFiles.getOrDefault("midi", null);
            
            boolean pdfExists = false;
            boolean midiExists = false;
            
            if (pdfPath != null) {
                Path fullPdfPath = fileStorageConfig.resolveFilePath(pdfPath);
                pdfExists = Files.exists(fullPdfPath);
            }
            
            if (midiPath != null) {
                Path fullMidiPath = fileStorageConfig.resolveFilePath(midiPath);
                midiExists = Files.exists(fullMidiPath);
            }
            
            // Step 5: List all files in the output directory
            Path outputDir = fileStorageConfig.getOutputDirectoryPath();
            File outputDirFile = outputDir.toFile();
            File[] files = outputDirFile.listFiles();
            List<String> fileNames = new ArrayList<>();
            
            if (files != null) {
                for (File file : files) {
                    fileNames.add(file.getName());
                }
            }
            
            response.put("success", pdfExists);
            response.put("id", id);
            response.put("testFile", testFileName);
            response.put("testFileExists", Files.exists(testFilePath));
            response.put("pdfPath", pdfPath);
            response.put("pdfExists", pdfExists);
            response.put("midiPath", midiPath);
            response.put("midiExists", midiExists);
            response.put("outputDirectory", outputDir.toString());
            response.put("filesInDirectory", fileNames);
            
            if (pdfExists) {
                response.put("pdfUrl", "/api/files/output/" + pdfPath);
            }
            
            if (midiExists) {
                response.put("midiUrl", "/api/files/output/" + midiPath);
            }
            
            logger.info("Pipeline test completed successfully for ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error testing pipeline: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}