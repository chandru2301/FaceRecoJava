package com.fr.attendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to manage face recognition lifecycle.
 * Provides a higher-level interface for starting/stopping recognition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceRecognitionService {

    private final RealTimeFaceRecognition realTimeFaceRecognition;

    /**
     * Starts the face recognition process.
     * 
     * @return true if started successfully, false otherwise
     */
    public boolean startRecognition() {
        log.info("Starting face recognition...");
        return realTimeFaceRecognition.startRecognition();
    }

    /**
     * Stops the face recognition process.
     */
    public void stopRecognition() {
        log.info("Stopping face recognition...");
        realTimeFaceRecognition.stopRecognition();
    }

    /**
     * Checks if face recognition is currently running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return realTimeFaceRecognition.isRunning();
    }
}
