package com.fr.attendance.controller;

import com.fr.attendance.service.ExcelService;
import com.fr.attendance.service.FaceRecognitionService;
import com.fr.attendance.service.PythonFaceRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing face recognition remotely.
 * Provides endpoints to start/stop recognition and check status.
 */
@Slf4j
@RestController
@RequestMapping("/api/recognition")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FaceRecognitionController {

    private final FaceRecognitionService faceRecognitionService;
    private final PythonFaceRecognitionService pythonFaceRecognitionService;
    private final ExcelService excelService;

    /**
     * Starts the face recognition process.
     * 
     * @param usePython Optional parameter to choose implementation (default: "java")
     * @return Response with status message
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startRecognition(
            @RequestParam(required = false, defaultValue = "java") String usePython) {
        
        Map<String, Object> response = new HashMap<>();

        if (faceRecognitionService.isRunning()) {
            response.put("success", false);
            response.put("message", "Face recognition is already running");
            response.put("running", true);
            return ResponseEntity.badRequest().body(response);
        }

        // For now, Python real-time recognition would require a different implementation
        // Currently, real-time only uses Java. Python is used for single image recognition.
        if ("python".equalsIgnoreCase(usePython)) {
            response.put("success", false);
            response.put("message", "Python real-time recognition is not yet implemented. " +
                      "Use Java implementation for real-time recognition, or use /recognize-image endpoint for single image recognition with Python.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean started = faceRecognitionService.startRecognition();
        
        // Check actual running status after starting
        boolean actuallyRunning = faceRecognitionService.isRunning();
        
        response.put("success", started && actuallyRunning);
        response.put("running", actuallyRunning);
        response.put("implementation", "java");
        
        if (!started || !actuallyRunning) {
            response.put("message", "Failed to start face recognition. " +
                          "Please ensure: 1) Model is trained, 2) Webcam is connected, 3) Model file exists.");
        } else {
            response.put("message", "Face recognition started successfully (Java implementation)");
        }

        return (started && actuallyRunning) 
            ? ResponseEntity.ok(response) 
            : ResponseEntity.internalServerError().body(response);
    }

    /**
     * Stops the face recognition process.
     * 
     * @return Response with status message
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopRecognition() {
        Map<String, Object> response = new HashMap<>();

        if (!faceRecognitionService.isRunning()) {
            response.put("success", false);
            response.put("message", "Face recognition is not running");
            return ResponseEntity.badRequest().body(response);
        }

        faceRecognitionService.stopRecognition();
        response.put("success", true);
        response.put("message", "Face recognition stopped successfully");
        response.put("running", false);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the current status of face recognition.
     * 
     * @return Response with current status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("running", faceRecognitionService.isRunning());
        response.put("message", faceRecognitionService.isRunning() 
                                ? "Face recognition is active" 
                                : "Face recognition is inactive");
        return ResponseEntity.ok(response);
    }

    /**
     * Recognize faces in an uploaded image using Python.
     * 
     * @param image The image file to recognize faces in
     * @param usePython Optional parameter (default: "python")
     * @return Recognition results with detected faces and their identities
     */
    @PostMapping("/recognize-image")
    public ResponseEntity<Map<String, Object>> recognizeImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(required = false, defaultValue = "python") String usePython) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (image.isEmpty()) {
            response.put("success", false);
            response.put("message", "Image file is required");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if Python is requested
        boolean usePythonImpl = "python".equalsIgnoreCase(usePython) 
                                && pythonFaceRecognitionService.isPythonAvailable();
        
        if (usePythonImpl) {
            try {
                // Save uploaded file temporarily
                Path tempFile = Files.createTempFile("recognition_", "_" + image.getOriginalFilename());
                image.transferTo(tempFile.toFile());
                
                // Recognize using Python
                Map<String, Object> result = pythonFaceRecognitionService.recognizeFace(tempFile.toString());
                
                // Cleanup temp file
                Files.deleteIfExists(tempFile);
                
                response.putAll(result);
                response.put("implementation", "python");
                return ResponseEntity.ok(response);
                
            } catch (IOException e) {
                log.error("Error processing image: {}", e.getMessage(), e);
                response.put("success", false);
                response.put("message", "Error processing image: " + e.getMessage());
                return ResponseEntity.internalServerError().body(response);
            }
        } else {
            response.put("success", false);
            response.put("message", "Python recognition not available. " +
                          "Please use Java implementation or install Python with face-recognition library.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get the location of the attendance Excel file.
     * 
     * @return Response with file path information
     */
    @GetMapping("/attendance-file")
    public ResponseEntity<Map<String, Object>> getAttendanceFileLocation() {
        Map<String, Object> response = new HashMap<>();
        String filePath = excelService.getExcelFilePath();
        java.io.File file = new java.io.File(filePath);
        
        response.put("filePath", filePath);
        response.put("exists", file.exists());
        response.put("absolutePath", file.getAbsolutePath());
        response.put("directory", file.getParent());
        
        if (file.exists()) {
            response.put("size", file.length());
            response.put("lastModified", new java.util.Date(file.lastModified()));
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Download the attendance Excel file.
     * 
     * @return Excel file as downloadable resource
     */
    @GetMapping("/attendance-file/download")
    public ResponseEntity<Resource> downloadAttendanceFile() {
        try {
            String filePath = excelService.getExcelFilePath();
            File file = new File(filePath);
            
            if (!file.exists()) {
                log.warn("Attendance file does not exist: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading attendance file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
