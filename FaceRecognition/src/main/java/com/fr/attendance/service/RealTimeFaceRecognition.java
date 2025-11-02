package com.fr.attendance.service;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.stereotype.Component;

import com.fr.attendance.model.DatabaseLabelNameMapper;
import com.fr.attendance.service.ExcelService;

import lombok.extern.slf4j.Slf4j;

/**
 * Real-time face recognition service using webcam.
 * Detects faces, recognizes them, and marks attendance automatically.
 */
@Slf4j
@Component
public class RealTimeFaceRecognition {

    private static final String MODEL_PATH = "trained_model.yml";
    private static final String CASCADE_PATH = "src/main/resources/haarcascade_frontalface_default.xml";
    private static final double CONFIDENCE_THRESHOLD = 80.0;

    private final ExcelService excelService;
    private final DatabaseLabelNameMapper labelNameMapper;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread recognitionThread;
    private FrameGrabber grabber;
    private CanvasFrame canvasFrame;
    private String resolvedModelPath; // Cache the resolved model path

    public RealTimeFaceRecognition(ExcelService excelService, DatabaseLabelNameMapper labelNameMapper) {
        this.excelService = excelService;
        this.labelNameMapper = labelNameMapper;
    }

    /**
     * Starts the real-time face recognition process.
     * 
     * @return true if started successfully, false otherwise
     */
    public boolean startRecognition() {
        if (isRunning.get()) {
            log.warn("Face recognition is already running");
            return false;
        }

        // Resolve model path (find it in common locations)
        resolvedModelPath = resolveModelPath();
        if (resolvedModelPath == null) {
            log.error("Model file not found. Please train the model first.");
            log.error("Searched in:");
            log.error("  - {}", new java.io.File(MODEL_PATH).getAbsolutePath());
            log.error("  - {}", new java.io.File("./" + MODEL_PATH).getAbsolutePath());
            log.error("  - {}", new java.io.File("../" + MODEL_PATH).getAbsolutePath());
            log.error("  - {}", new java.io.File(System.getProperty("user.dir") + "/" + MODEL_PATH).getAbsolutePath());
            log.error("Current working directory: {}", System.getProperty("user.dir"));
            return false;
        }
        log.info("✅ Model file found at: {}", resolvedModelPath);

        recognitionThread = new Thread(this::runRecognition);
        recognitionThread.setDaemon(true);
        recognitionThread.start();

        // Wait a bit for thread to initialize and check if it actually started
        try {
            Thread.sleep(500);
            if (!isRunning.get()) {
                log.error("Recognition thread failed to start properly. Check logs above for detailed error messages.");
                log.error("Possible issues:");
                log.error("  1. Webcam not connected or in use by another application");
                log.error("  2. Cascade classifier file not found");
                log.error("  3. Error loading model file");
                log.error("  4. Permission denied accessing webcam");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }

    /**
     * Stops the real-time face recognition process.
     */
    public void stopRecognition() {
        if (!isRunning.get()) {
            log.warn("Face recognition is not running");
            return;
        }

        isRunning.set(false);

        if (recognitionThread != null) {
            try {
                recognitionThread.join(3000);
            } catch (InterruptedException e) {
                log.error("Error stopping recognition thread: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

        cleanup();
        log.info("Face recognition stopped");
    }

    /**
     * Checks if recognition is currently running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Resolves the model file path by checking common locations.
     * 
     * @return absolute path to the model file, or null if not found
     */
    private String resolveModelPath() {
        String userDir = System.getProperty("user.dir");
        String[] possiblePaths = {
            MODEL_PATH,
            "./" + MODEL_PATH,
            "../" + MODEL_PATH,
            userDir + java.io.File.separator + MODEL_PATH,
            userDir + java.io.File.separator + ".." + java.io.File.separator + MODEL_PATH
        };
        
        for (String path : possiblePaths) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.isFile()) {
                String absPath = file.getAbsolutePath();
                log.debug("Found model at: {}", absPath);
                return absPath;
            }
        }
        
        return null;
    }

    /**
     * Main recognition loop that processes webcam frames.
     */
    private void runRecognition() {
        try {
            log.info("Initializing face detector cascade...");
            java.io.File cascadeFile = new java.io.File(CASCADE_PATH);
            if (!cascadeFile.exists()) {
                log.error("Cascade file not found at: {} (absolute: {})", 
                         CASCADE_PATH, cascadeFile.getAbsolutePath());
                isRunning.set(false);
                return;
            }
            
            CascadeClassifier faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (faceDetector.empty()) {
                log.error("Could not load face detector cascade from: {} (absolute: {})", 
                         CASCADE_PATH, cascadeFile.getAbsolutePath());
                isRunning.set(false);
                return;
            }
            log.info("Face detector cascade loaded successfully");

            // Use the resolved model path (from startRecognition check)
            if (resolvedModelPath == null) {
                resolvedModelPath = resolveModelPath();
            }
            
            if (resolvedModelPath == null) {
                log.error("Model file not found. Cannot start recognition.");
                isRunning.set(false);
                return;
            }
            
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            log.info("Loading model from: {}", resolvedModelPath);
            recognizer.read(resolvedModelPath);
            log.info("Model loaded successfully");

            log.info("Attempting to open webcam (device 0)...");
            grabber = new OpenCVFrameGrabber(0);
            try {
                grabber.start();
                log.info("✅ Webcam initialized and started successfully");
            } catch (Exception e) {
                log.error("❌ Failed to start webcam: {}", e.getMessage(), e);
                log.error("Possible causes:");
                log.error("  - Webcam not connected");
                log.error("  - Webcam in use by another application");
                log.error("  - Permission denied");
                log.error("  - Wrong device index (try changing 0 to 1 or 2)");
                isRunning.set(false);
                return;
            }

            // Try to force GUI mode even if headless is set
            // This works on Windows with a desktop environment
            boolean headlessMode = java.awt.GraphicsEnvironment.isHeadless();
            
            // On Windows, try to enable GUI even if headless property is set
            if (headlessMode && System.getProperty("os.name").toLowerCase().contains("windows")) {
                try {
                    // Try to set system property to disable headless (if not already set)
                    String headlessProp = System.getProperty("java.awt.headless");
                    if ("true".equals(headlessProp)) {
                        log.info("Attempting to enable GUI mode on Windows...");
                        System.setProperty("java.awt.headless", "false");
                        // Re-check after setting property
                        headlessMode = java.awt.GraphicsEnvironment.isHeadless();
                        log.info("Headless mode after property change: {}", headlessMode);
                    }
                } catch (Exception e) {
                    log.warn("Could not modify headless property: {}", e.getMessage());
                }
            }
            
            if (!headlessMode) {
                try {
                    canvasFrame = new CanvasFrame("Real-Time Face Recognition", 1);
                    canvasFrame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
                    log.info("✅ GUI window opened for face recognition display");
                } catch (Exception e) {
                    log.warn("Could not create GUI window, running in headless mode: {}", e.getMessage());
                    headlessMode = true;
                }
            } else {
                log.info("Running in headless mode (no GUI display available)");
                log.info("Face recognition will still work - check logs for attendance marks");
            }

            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            Set<String> markedToday = excelService.getMarkedToday();

            // Load label mappings from database
            labelNameMapper.loadMappings();

            // Set running flag after successful initialization
            isRunning.set(true);
            log.info("Face recognition started successfully");
            
            int frameCount = 0;
            long lastLogTime = System.currentTimeMillis();

            while (isRunning.get() && (headlessMode || (canvasFrame != null && canvasFrame.isVisible()))) {
                Frame videoFrame = grabber.grab();
                if (videoFrame == null) {
                    log.warn("Received null frame from webcam");
                    try {
                        Thread.sleep(100); // Wait a bit before retrying
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                frameCount++;
                long currentTime = System.currentTimeMillis();
                
                // Log every 5 seconds that we're processing frames
                if (currentTime - lastLogTime > 5000) {
                    log.info("Processing frames... (processed {} frames in last 5 seconds)", frameCount);
                    frameCount = 0;
                    lastLogTime = currentTime;
                }

                Mat mat = converter.convert(videoFrame);
                Mat gray = new Mat();
                cvtColor(mat, gray, COLOR_BGR2GRAY);

                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(gray, faces);
                
                // Log face detection (even if no faces found, periodically)
                if (faces.size() > 0) {
                    log.info("👤 Detected {} face(s) in frame", faces.size());
                } else if (frameCount % 150 == 0) { // Log every ~5 seconds if no faces (at 30fps)
                    log.info("⏳ No faces detected - make sure you're facing the camera and well-lit");
                }

                for (int i = 0; i < faces.size(); i++) {
                    Rect rect = faces.get(i);
                    
                    // Draw rectangle around detected face
                    rectangle(mat, rect, new Scalar(0, 255, 0, 0), 2, 0, 0);

                    Mat face = new Mat(gray, rect);
                    resize(face, face, new Size(200, 200));

                    int[] label = new int[1];
                    double[] confidence = new double[1];
                    recognizer.predict(face, label, confidence);

                    String name;
                    Scalar color;

                    // Log all recognition attempts for debugging
                    log.info("🔍 Recognition attempt - Label: {}, Confidence: {}, Threshold: {}", 
                            label[0], String.format("%.2f", confidence[0]), String.format("%.2f", CONFIDENCE_THRESHOLD));

                    if (confidence[0] < CONFIDENCE_THRESHOLD && labelNameMapper.containsLabel(label[0])) {
                        name = labelNameMapper.getName(label[0]);
                        String department = labelNameMapper.getDepartment(label[0]);
                        color = new Scalar(0, 255, 0, 0); // Green for recognized

                        // Log recognition success
                        log.info("✅ Recognized: {} (department: {}, confidence: {})", 
                                name, department, String.format("%.2f", confidence[0]));

                        // Mark attendance if not already marked today
                        if (!markedToday.contains(name)) {
                            boolean marked = excelService.markAttendance(name, department, "Present");
                            if (marked) {
                                markedToday.add(name);
                                log.info("📝 {} ({}) marked Present in Excel at: {}", 
                                        name, department, excelService.getExcelFilePath());
                            } else {
                                // Attendance was not marked - either already marked or error occurred
                                // Add to set anyway to prevent repeated attempts
                                markedToday.add(name);
                                log.debug("ℹ️ {} attendance was not marked (already marked or error)", name);
                            }
                        } else {
                            log.debug("ℹ️ {} already marked today (in-memory check), skipping", name);
                        }
                    } else {
                        name = "Unknown";
                        color = new Scalar(0, 0, 255, 0); // Red for unknown
                        
                        if (labelNameMapper.containsLabel(label[0])) {
                            log.warn("❌ Face detected but confidence too high: {} (threshold: {})", 
                                    String.format("%.2f", confidence[0]), String.format("%.2f", CONFIDENCE_THRESHOLD));
                        } else {
                            log.info("❓ Unknown face - Label {} not in database (confidence: {})", 
                                    label[0], String.format("%.2f", confidence[0]));
                        }
                    }

                    // Draw name and confidence on frame
                    String displayText = String.format("%s (%.1f)", name, confidence[0]);
                    putText(mat, displayText, new Point(rect.x(), rect.y() - 10),
                            FONT_HERSHEY_SIMPLEX, 0.7, color);
                }

                // Only show image if GUI is available
                if (!headlessMode && canvasFrame != null) {
                    canvasFrame.showImage(converter.convert(mat));
                } else {
                    // In headless mode, add a small delay to avoid excessive CPU usage
                    try {
                        Thread.sleep(33); // ~30 FPS
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error during face recognition: {}", e.getMessage(), e);
        } finally {
            cleanup();
            isRunning.set(false);
            log.info("Face recognition stopped");
        }
    }

    /**
     * Cleans up resources (grabber, frame, etc.).
     */
    private void cleanup() {
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (Exception e) {
            log.error("Error stopping grabber: {}", e.getMessage(), e);
        }

        if (canvasFrame != null) {
            canvasFrame.dispose();
        }
    }
}
