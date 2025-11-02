package com.fr.attendance.controller;

import com.fr.attendance.service.DatabaseFaceTrainer;
import com.fr.attendance.service.PythonFaceRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for training face recognition models from registered students.
 * Supports both Java (OpenCV) and Python implementations.
 */
@Slf4j
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrainingController {

    private final DatabaseFaceTrainer databaseFaceTrainer;
    private final PythonFaceRecognitionService pythonFaceRecognitionService;

    /**
     * Trains the face recognition model using all registered students from the database.
     * Uses Python implementation by default, falls back to Java if Python is unavailable.
     * 
     * @param usePython Optional parameter to force Python/Java (default: auto-detect)
     * @return Response with training status
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> trainModel(
            @RequestParam(required = false, defaultValue = "auto") String usePython) {
        Map<String, Object> response = new HashMap<>();

        // Determine which implementation to use
        boolean usePythonImpl = false;
        if ("python".equalsIgnoreCase(usePython)) {
            usePythonImpl = pythonFaceRecognitionService.isPythonAvailable();
            if (!usePythonImpl) {
                response.put("success", false);
                response.put("message", "Python not available. Please install Python and required packages.");
                return ResponseEntity.badRequest().body(response);
            }
        } else if ("java".equalsIgnoreCase(usePython)) {
            usePythonImpl = false;
        } else {
            // Auto-detect: prefer Python if available
            usePythonImpl = pythonFaceRecognitionService.isPythonAvailable();
        }

        if (usePythonImpl) {
            log.info("Training model using Python implementation");
            Map<String, Object> result = pythonFaceRecognitionService.trainModel();
            return ResponseEntity.ok(result);
        } else {
            log.info("Training model using Java (OpenCV) implementation");
            boolean success = databaseFaceTrainer.trainFromDatabase();
            response.put("success", success);
            response.put("message", success 
                         ? "Model trained successfully from database (Java)" 
                         : "Failed to train model. Please ensure students are registered.");
            response.put("implementation", "java");

            return success ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Checks Python availability.
     * 
     * @return Python availability status
     */
    @GetMapping("/python-status")
    public ResponseEntity<Map<String, Object>> checkPythonStatus() {
        Map<String, Object> response = new HashMap<>();
        boolean available = pythonFaceRecognitionService.isPythonAvailable();
        response.put("pythonAvailable", available);
        response.put("message", available 
                     ? "Python is available and ready to use" 
                     : "Python is not available. Using Java implementation.");
        return ResponseEntity.ok(response);
    }
}
