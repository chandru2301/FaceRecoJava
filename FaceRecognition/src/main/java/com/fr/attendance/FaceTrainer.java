package com.fr.attendance;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_face;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

/**
 * FaceTrainer - Trains the LBPH face recognizer using face images from the faces directory.
 * Each person should have a folder with their name containing multiple face images.
 */
public class FaceTrainer {

    private static final String TRAINING_DIR = "faces/";
    private static final String CASCADE_PATH = "src/main/resources/haarcascade_frontalface_default.xml";
    private static final String MODEL_PATH = "trained_model.yml";
    private static final String NAMES_PATH = "label_names.txt";

    public static void main(String[] args) {
        try {
            trainFaces();
        } catch (Exception e) {
            System.err.println("Error during training: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void trainFaces() throws IOException {
        CascadeClassifier faceDetector = new CascadeClassifier(CASCADE_PATH);
        if (faceDetector.empty()) {
            throw new IOException("Could not load face detector cascade from: " + CASCADE_PATH);
        }

        List<Mat> images = new ArrayList<>();
        List<Integer> labelsList = new ArrayList<>();
        int counter = 0;
        List<String> names = new ArrayList<>();

        File root = new File(TRAINING_DIR);
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("Training directory not found: " + TRAINING_DIR);
            System.err.println("Please create the directory and add subdirectories for each person.");
            return;
        }

        File[] personDirs = root.listFiles();
        if (personDirs == null || personDirs.length == 0) {
            System.err.println("No person directories found in: " + TRAINING_DIR);
            return;
        }

        for (File personDir : personDirs) {
            if (!personDir.isDirectory()) {
                continue;
            }

            String name = personDir.getName();
            names.add(name);
            System.out.println("Processing: " + name);

            File[] imageFiles = personDir.listFiles((dir, filename) -> {
                String lower = filename.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                       lower.endsWith(".png") || lower.endsWith(".bmp");
            });

            if (imageFiles == null || imageFiles.length == 0) {
                System.err.println("No images found in: " + personDir.getAbsolutePath());
                continue;
            }

            int faceCount = 0;
            for (File imageFile : imageFiles) {
                Mat img = opencv_imgcodecs.imread(imageFile.getAbsolutePath(), 
                                                  opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (img.empty()) {
                    System.err.println("Could not load image: " + imageFile.getAbsolutePath());
                    continue;
                }

                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(img, faces);

                if (faces.size() == 0) {
                    System.err.println("No face detected in: " + imageFile.getName());
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
                    labelsList.add(counter);
                    faceCount++;
                }
            }

            System.out.println("  Found " + faceCount + " faces for " + name);
            counter++;
        }

        if (images.isEmpty()) {
            System.err.println("No faces found for training!");
            return;
        }

        // Convert labels list to int array
        int[] labelsArray = new int[labelsList.size()];
        for (int i = 0; i < labelsList.size(); i++) {
            labelsArray[i] = labelsList.get(i);
        }

        // ✅ Convert to MatVector (must be done manually to avoid native crash)
        MatVector matVector = new MatVector(images.size());
        for (int i = 0; i < images.size(); i++) {
            matVector.put(i, images.get(i));
        }

        // ✅ Convert labels to Mat (CV_32SC1)
        Mat labels = new Mat(labelsArray.length, 1, opencv_core.CV_32SC1);
        IntBuffer labelsBuffer = labels.createBuffer();
        labelsBuffer.put(labelsArray);

        // ✅ Create and train recognizer
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        recognizer.train(matVector, labels);

        // ✅ Save model
        recognizer.save(MODEL_PATH);

        // Save names list to file
        try (java.io.FileWriter writer = new java.io.FileWriter(NAMES_PATH)) {
            for (int i = 0; i < names.size(); i++) {
                writer.write(i + "=" + names.get(i) + "\n");
            }
        }

        System.out.println("\n✅ Training complete!");
        System.out.println("Total faces trained: " + images.size());
        System.out.println("Total persons: " + names.size());
        System.out.println("Names: " + names);
        System.out.println("Model saved to: " + MODEL_PATH);
        System.out.println("Names saved to: " + NAMES_PATH);
    }
}

