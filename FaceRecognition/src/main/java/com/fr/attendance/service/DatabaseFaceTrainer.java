package com.fr.attendance.service;

import com.fr.attendance.entity.Student;
import com.fr.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for training face recognition model from database-stored student images.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseFaceTrainer {

    private static final String CASCADE_PATH = "src/main/resources/haarcascade_frontalface_default.xml";
    private static final String MODEL_PATH = "trained_model.yml";
    private static final String NAMES_PATH = "label_names.txt";
    
    private final StudentRepository studentRepository;

    /**
     * Trains the face recognition model using all registered students from the database.
     * 
     * @return true if training was successful, false otherwise
     */
    public boolean trainFromDatabase() {
        try {
            List<Student> students = studentRepository.findAll();
            
            if (students.isEmpty()) {
                log.warn("No students found in database. Please register students first.");
                return false;
            }

            CascadeClassifier faceDetector = new CascadeClassifier(CASCADE_PATH);
            if (faceDetector.empty()) {
                log.error("Could not load face detector cascade from: {}", CASCADE_PATH);
                return false;
            }

            List<Mat> images = new ArrayList<>();
            List<Integer> labelsList = new ArrayList<>();
            List<String> names = new ArrayList<>();

            for (Student student : students) {
                log.info("Processing student: {} (Department: {})", student.getName(), student.getDepartment());
                names.add(student.getName());

                // Load student image
                java.io.File imageFile = new java.io.File(student.getImagePath());
                if (!imageFile.exists()) {
                    log.warn("Image file not found: {}", student.getImagePath());
                    continue;
                }

                Mat img = opencv_imgcodecs.imread(imageFile.getAbsolutePath(), 
                                                  opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (img.empty()) {
                    log.warn("Could not load image: {}", imageFile.getAbsolutePath());
                    continue;
                }

                // Detect face in image
                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(img, faces);

                if (faces.size() == 0) {
                    log.warn("No face detected in image for student: {}", student.getName());
                    continue;
                }

                // Use the largest face if multiple faces detected
                Rect largestFace = null;
                int maxArea = 0;
                for (int i = 0; i < faces.size(); i++) {
                    Rect rect = faces.get(i);
                    int area = rect.width() * rect.height();
                    if (area > maxArea) {
                        maxArea = area;
                        largestFace = rect;
                    }
                }

                if (largestFace != null) {
                    Mat face = new Mat(img, largestFace);
                    opencv_imgproc.resize(face, face, new Size(200, 200));
                    images.add(face);
                    labelsList.add(student.getLabelId());
                    log.info("  Found face for {} (Label ID: {})", student.getName(), student.getLabelId());
                }
            }

            if (images.isEmpty()) {
                log.error("No faces found for training!");
                return false;
            }

            // Convert labels list to int array
            int[] labelsArray = new int[labelsList.size()];
            for (int i = 0; i < labelsList.size(); i++) {
                labelsArray[i] = labelsList.get(i);
            }

            // Convert to MatVector (must be done manually to avoid native crash)
            MatVector matVector = new MatVector(images.size());
            for (int i = 0; i < images.size(); i++) {
                matVector.put(i, images.get(i));
            }

            // Convert labels to Mat (CV_32SC1)
            Mat labels = new Mat(labelsArray.length, 1, opencv_core.CV_32SC1);
            IntBuffer labelsBuffer = labels.createBuffer();
            labelsBuffer.put(labelsArray);

            // Create and train recognizer
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.train(matVector, labels);

            // ✅ Save model (ensure absolute path for reliability)
            java.io.File modelFile = new java.io.File(MODEL_PATH);
            String absoluteModelPath = modelFile.getAbsolutePath();
            recognizer.save(absoluteModelPath);
            
            // Verify model was saved
            java.io.File savedModel = new java.io.File(absoluteModelPath);
            if (savedModel.exists()) {
                log.info("✅ Model verified at: {}", absoluteModelPath);
            } else {
                log.warn("⚠️ Model save may have failed. File not found at: {}", absoluteModelPath);
            }

            // Save names mapping to file for compatibility
            java.io.File namesFile = new java.io.File(NAMES_PATH);
            try (FileWriter writer = new FileWriter(namesFile.getAbsolutePath())) {
                for (int i = 0; i < names.size(); i++) {
                    Student student = students.get(i);
                    writer.write(student.getLabelId() + "=" + student.getName() + "\n");
                }
            }

            log.info("✅ Training complete!");
            log.info("Total faces trained: {}", images.size());
            log.info("Total students: {}", students.size());
            log.info("Model saved to: {} (absolute: {})", MODEL_PATH, absoluteModelPath);
            log.info("Names saved to: {} (absolute: {})", NAMES_PATH, namesFile.getAbsolutePath());

            return true;

        } catch (Exception e) {
            log.error("Error during training: {}", e.getMessage(), e);
            return false;
        }
    }
}
