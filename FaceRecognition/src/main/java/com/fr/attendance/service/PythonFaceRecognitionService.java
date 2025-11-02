package com.fr.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fr.attendance.entity.Student;
import com.fr.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that integrates Python face recognition with Spring Boot.
 * Executes Python scripts for face training and recognition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonFaceRecognitionService {

    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PYTHON_SCRIPT = "python/face_recognition_service.py";
    private static final String PYTHON_EXECUTABLE = findPythonExecutable();

    /**
     * Trains the face recognition model using Python.
     * 
     * @return Training result
     */
    public Map<String, Object> trainModel() {
        try {
            List<Student> students = studentRepository.findAll();
            
            if (students.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "No students found in database"
                );
            }

            // Prepare student data for Python script
            List<Map<String, Object>> studentsData = students.stream()
                .map(student -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", student.getId());
                    data.put("name", student.getName());
                    data.put("department", student.getDepartment());
                    // Normalize path separators for cross-platform compatibility
                    String imagePath = student.getImagePath();
                    if (imagePath != null) {
                        imagePath = imagePath.replace('\\', '/');
                    }
                    data.put("imagePath", imagePath);
                    data.put("labelId", student.getLabelId());
                    return data;
                })
                .collect(Collectors.toList());

            // Create temporary JSON file
            Path tempJson = Files.createTempFile("students_", ".json");
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(tempJson.toFile(), studentsData);

            // Execute Python script
            ProcessBuilder processBuilder = new ProcessBuilder(
                PYTHON_EXECUTABLE,
                PYTHON_SCRIPT,
                "train",
                tempJson.toString()
            );

            // Separate stdout and stderr streams
            processBuilder.redirectErrorStream(false);
            Process process = processBuilder.start();

            // Read stdout (JSON response)
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // Read stdout in separate thread
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading Python stdout: {}", e.getMessage(), e);
                }
            });
            
            // Read stderr in separate thread
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        log.debug("Python stderr: {}", line);
                    }
                } catch (IOException e) {
                    log.error("Error reading Python stderr: {}", e.getMessage(), e);
                }
            });
            
            stdoutReader.start();
            stderrReader.start();
            
            int exitCode = process.waitFor();
            
            // Wait for threads to finish reading
            stdoutReader.join(2000);
            stderrReader.join(2000);

            // Cleanup
            Files.deleteIfExists(tempJson);

            if (exitCode != 0) {
                log.error("Python script failed with exit code: {}", exitCode);
                if (errorOutput.length() > 0) {
                    log.error("Python error output: {}", errorOutput);
                }
                if (output.length() > 0) {
                    log.error("Python stdout: {}", output);
                }
                return Map.of(
                    "success", false,
                    "message", "Training failed. Check logs for details."
                );
            }

            // Parse JSON response - only use lines that look like JSON
            String jsonOutput = output.toString().trim();
            
            // Filter out non-JSON lines (warnings, etc.)
            String[] lines = jsonOutput.split("\n");
            StringBuilder jsonOnly = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("{") || line.startsWith("[")) {
                    jsonOnly.append(line);
                }
            }
            
            if (jsonOnly.length() == 0) {
                log.error("No valid JSON found in Python output. Output: {}", output);
                log.error("Error output: {}", errorOutput);
                return Map.of(
                    "success", false,
                    "message", "Python script did not return valid JSON"
                );
            }
            
            Map<String, Object> result = objectMapper.readValue(
                jsonOnly.toString(), 
                Map.class
            );

            log.info("Python training completed: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error training model with Python: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Training error: " + e.getMessage()
            );
        }
    }

    /**
     * Recognizes a face in an image file.
     * 
     * @param imagePath Path to the image file
     * @return Recognition results
     */
    public Map<String, Object> recognizeFace(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                return Map.of(
                    "success", false,
                    "message", "Image file not found"
                );
            }

            // Execute Python script
            ProcessBuilder processBuilder = new ProcessBuilder(
                PYTHON_EXECUTABLE,
                PYTHON_SCRIPT,
                "recognize",
                imagePath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Python recognition failed with exit code: {}", exitCode);
                return Map.of(
                    "success", false,
                    "message", "Recognition failed"
                );
            }

            // Parse JSON response
            String jsonOutput = output.toString().trim();
            Map<String, Object> result = objectMapper.readValue(
                jsonOutput,
                Map.class
            );

            return result;

        } catch (Exception e) {
            log.error("Error recognizing face with Python: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Recognition error: " + e.getMessage()
            );
        }
    }

    /**
     * Finds Python executable in the system.
     * 
     * @return Python executable path
     */
    private static String findPythonExecutable() {
        String[] pythonCommands = {"python3", "python", "py"};
        
        for (String cmd : pythonCommands) {
            try {
                Process process = new ProcessBuilder(cmd, "--version").start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    log.info("Found Python executable: {}", cmd);
                    return cmd;
                }
            } catch (Exception e) {
                // Try next command
            }
        }
        
        log.warn("Python not found. Defaulting to 'python'");
        return "python";
    }

    /**
     * Checks if Python is available.
     * 
     * @return true if Python is available
     */
    public boolean isPythonAvailable() {
        try {
            Process process = new ProcessBuilder(PYTHON_EXECUTABLE, "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

