"""
Face Recognition Service - Python implementation for Spring Boot integration
Handles face training and recognition operations
"""

import os
import sys
import json
import cv2
import numpy as np
import pickle
from pathlib import Path
import base64
import warnings

# Suppress all warnings to stderr to keep stdout clean for JSON
warnings.filterwarnings('ignore')

# Redirect OpenCV warnings to stderr (environment variable only, cv2.setLogLevel varies by version)
os.environ['OPENCV_LOG_LEVEL'] = 'ERROR'

# Try to import face_recognition library
try:
    import face_recognition
    FACE_RECOGNITION_AVAILABLE = True
except ImportError:
    FACE_RECOGNITION_AVAILABLE = False
    # Print to stderr, not stdout
    print("Warning: face_recognition library not found. Using OpenCV fallback.", file=sys.stderr)

class FaceRecognitionService:
    def __init__(self, model_dir="python/models"):
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(parents=True, exist_ok=True)
        self.model_path = self.model_dir / "face_recognizer.pkl"
        self.encodings_path = self.model_dir / "face_encodings.pkl"
        self.labels_path = self.model_dir / "labels.pkl"
        self.cascade_path = self.model_dir / "haarcascade_frontalface_default.xml"
        
        self.recognizer = None
        self.known_encodings = []
        self.known_labels = []
        self.student_data = {}
    
    def _normalize_path(self, path_str):
        """Normalize Windows paths (backslashes to forward slashes)"""
        if path_str:
            # Replace backslashes with forward slashes for cross-platform compatibility
            return str(path_str).replace('\\', '/')
        return path_str
        
    def train_from_database(self, students_data):
        """
        Train face recognition model from student data
        Args:
            students_data: List of dicts with keys: id, name, department, imagePath
        Returns:
            dict with success status and message
        """
        try:
            self.known_encodings = []
            self.known_labels = []
            self.student_data = {}
            
            if FACE_RECOGNITION_AVAILABLE:
                return self._train_with_face_recognition(students_data)
            else:
                # Try OpenCV as fallback
                result = self._train_with_opencv(students_data)
                # If OpenCV also fails, provide helpful error message
                if not result.get("success"):
                    error_msg = result.get("message", "")
                    if "not available" in error_msg.lower() or "cannot import" in error_msg.lower():
                        result["message"] = (
                            "Python face recognition libraries not installed.\n"
                            "Options:\n"
                            "1. Install face-recognition (recommended): pip install face-recognition\n"
                            "2. Install opencv-contrib-python: pip install opencv-contrib-python\n"
                            "3. Use Java implementation: /api/training/train?usePython=java"
                        )
                return result
                
        except Exception as e:
            error_msg = str(e)
            if "cannot import name 'face'" in error_msg or "cv2.face" in error_msg:
                return {
                    "success": False,
                    "message": (
                        "OpenCV face module not available. Install opencv-contrib-python:\n"
                        "  pip uninstall opencv-python\n"
                        "  pip install opencv-contrib-python\n"
                        "Or use Java implementation: /api/training/train?usePython=java"
                    )
                }
            return {"success": False, "message": f"Training error: {error_msg}"}
    
    def _train_with_face_recognition(self, students_data):
        """Train using face_recognition library"""
        trained_count = 0
        
        for student in students_data:
            image_path = self._normalize_path(student.get('imagePath'))
            student_id = student.get('id')
            name = student.get('name')
            department = student.get('department')
            label_id = student.get('labelId')
            
            if not image_path or not os.path.exists(image_path):
                print(f"Image not found: {image_path}", file=sys.stderr)
                continue
            
            try:
                # Load image
                image = face_recognition.load_image_file(image_path)
                encodings = face_recognition.face_encodings(image)
                
                if len(encodings) > 0:
                    # Use the first face found
                    encoding = encodings[0]
                    self.known_encodings.append(encoding)
                    self.known_labels.append(label_id)
                    self.student_data[label_id] = {
                        'id': student_id,
                        'name': name,
                        'department': department
                    }
                    trained_count += 1
                    
            except Exception as e:
                # All errors to stderr, never stdout
                print(f"Error processing {image_path}: {str(e)}", file=sys.stderr)
                continue
        
        if trained_count == 0:
            return {"success": False, "message": "No faces found in training images"}
        
        # Save model
        self._save_model()
        
        return {
            "success": True,
            "message": f"Trained {trained_count} faces successfully",
            "trained_count": trained_count
        }
    
    def _train_with_opencv(self, students_data):
        """Train using OpenCV LBPHFaceRecognizer as fallback"""
        try:
            import cv2
            # Try to import face module (requires opencv-contrib-python)
            try:
                from cv2 import face
            except ImportError:
                return {
                    "success": False,
                    "message": "OpenCV face module not available. Please install opencv-contrib-python: pip install opencv-contrib-python"
                }
            
            recognizer = face.LBPHFaceRecognizer_create()
            images = []
            labels = []
            label_to_student = {}
            
            for student in students_data:
                image_path = self._normalize_path(student.get('imagePath'))
                label_id = student.get('labelId')
                student_id = student.get('id')
                name = student.get('name')
                department = student.get('department')
                
                if not image_path or not os.path.exists(image_path):
                    print(f"Image not found: {image_path}", file=sys.stderr)
                    continue
                
                try:
                    # Load and preprocess image
                    img = cv2.imread(image_path)
                    if img is None:
                        continue
                    
                    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
                    
                    # Detect faces
                    face_cascade = cv2.CascadeClassifier(str(self.cascade_path))
                    if face_cascade.empty():
                        # Try to find cascade in resources
                        cascade_paths = [
                            "src/main/resources/haarcascade_frontalface_default.xml",
                            "../src/main/resources/haarcascade_frontalface_default.xml"
                        ]
                        for path in cascade_paths:
                            if os.path.exists(path):
                                face_cascade = cv2.CascadeClassifier(path)
                                break
                    
                    faces = face_cascade.detectMultiScale(gray, 1.1, 5)
                    
                    if len(faces) > 0:
                        # Use largest face
                        largest_face = max(faces, key=lambda x: x[2] * x[3])
                        x, y, w, h = largest_face
                        face_roi = gray[y:y+h, x:x+w]
                        face_roi = cv2.resize(face_roi, (200, 200))
                        
                        images.append(face_roi)
                        labels.append(label_id)
                        label_to_student[label_id] = {
                            'id': student_id,
                            'name': name,
                            'department': department
                        }
                        
                except Exception as e:
                    print(f"Error processing {image_path}: {str(e)}", file=sys.stderr)
                    continue
            
            if len(images) == 0:
                return {"success": False, "message": "No faces found in training images"}
            
            # Train recognizer
            recognizer.train(images, np.array(labels))
            
            # Save model
            recognizer.save(str(self.model_path))
            self.recognizer = recognizer
            self.student_data = label_to_student
            
            # Save labels mapping
            with open(self.labels_path, 'wb') as f:
                pickle.dump(label_to_student, f)
            
            return {
                "success": True,
                "message": f"Trained {len(images)} faces successfully",
                "trained_count": len(images)
            }
            
        except Exception as e:
            return {"success": False, "message": f"OpenCV training error: {str(e)}"}
    
    def recognize_face(self, image_path):
        """
        Recognize a face in an image
        Args:
            image_path: Path to image file
        Returns:
            dict with recognition results
        """
        try:
            if not os.path.exists(image_path):
                return {"success": False, "message": "Image file not found"}
            
            if FACE_RECOGNITION_AVAILABLE and self.known_encodings:
                return self._recognize_with_face_recognition(image_path)
            elif self.recognizer:
                return self._recognize_with_opencv(image_path)
            else:
                return {"success": False, "message": "Model not trained"}
                
        except Exception as e:
            return {"success": False, "message": f"Recognition error: {str(e)}"}
    
    def _recognize_with_face_recognition(self, image_path):
        """Recognize using face_recognition library"""
        try:
            # Load and find faces
            image = face_recognition.load_image_file(image_path)
            face_locations = face_recognition.face_locations(image)
            face_encodings = face_recognition.face_encodings(image, face_locations)
            
            if len(face_encodings) == 0:
                return {"success": False, "message": "No face detected"}
            
            results = []
            for face_encoding, face_location in zip(face_encodings, face_locations):
                # Compare with known faces
                matches = face_recognition.compare_faces(self.known_encodings, face_encoding, tolerance=0.6)
                face_distances = face_recognition.face_distance(self.known_encodings, face_encoding)
                
                best_match_index = np.argmin(face_distances)
                confidence = 1 - face_distances[best_match_index]
                
                if matches[best_match_index] and confidence > 0.5:
                    label_id = self.known_labels[best_match_index]
                    student = self.student_data.get(label_id)
                    
                    if student:
                        results.append({
                            "labelId": label_id,
                            "name": student['name'],
                            "department": student['department'],
                            "confidence": float(confidence),
                            "location": face_location
                        })
                    else:
                        results.append({
                            "labelId": -1,
                            "name": "Unknown",
                            "department": "",
                            "confidence": float(confidence),
                            "location": face_location
                        })
                else:
                    results.append({
                        "labelId": -1,
                        "name": "Unknown",
                        "department": "",
                        "confidence": float(confidence),
                        "location": face_location
                    })
            
            return {"success": True, "results": results}
            
        except Exception as e:
            return {"success": False, "message": f"Recognition error: {str(e)}"}
    
    def _recognize_with_opencv(self, image_path):
        """Recognize using OpenCV LBPHFaceRecognizer"""
        try:
            import cv2
            
            img = cv2.imread(image_path)
            if img is None:
                return {"success": False, "message": "Could not load image"}
            
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Detect faces
            face_cascade = cv2.CascadeClassifier(str(self.cascade_path))
            if face_cascade.empty():
                cascade_paths = [
                    "src/main/resources/haarcascade_frontalface_default.xml",
                    "../src/main/resources/haarcascade_frontalface_default.xml"
                ]
                for path in cascade_paths:
                    if os.path.exists(path):
                        face_cascade = cv2.CascadeClassifier(path)
                        break
            
            faces = face_cascade.detectMultiScale(gray, 1.1, 5)
            
            if len(faces) == 0:
                return {"success": False, "message": "No face detected"}
            
            results = []
            for (x, y, w, h) in faces:
                face_roi = gray[y:y+h, x:x+w]
                face_roi = cv2.resize(face_roi, (200, 200))
                
                label_id, confidence = self.recognizer.predict(face_roi)
                
                # LBPH returns lower confidence for better matches
                confidence_score = (100 - confidence) / 100.0
                
                if confidence_score > 0.5 and label_id in self.student_data:
                    student = self.student_data[label_id]
                    results.append({
                        "labelId": int(label_id),
                        "name": student['name'],
                        "department": student['department'],
                        "confidence": float(confidence_score),
                        "location": (int(y), int(x+w), int(y+h), int(x))
                    })
                else:
                    results.append({
                        "labelId": -1,
                        "name": "Unknown",
                        "department": "",
                        "confidence": float(confidence_score),
                        "location": (int(y), int(x+w), int(y+h), int(x))
                    })
            
            return {"success": True, "results": results}
            
        except Exception as e:
            return {"success": False, "message": f"Recognition error: {str(e)}"}
    
    def _save_model(self):
        """Save trained model"""
        try:
            with open(self.encodings_path, 'wb') as f:
                pickle.dump(self.known_encodings, f)
            
            with open(self.labels_path, 'wb') as f:
                pickle.dump({
                    'labels': self.known_labels,
                    'student_data': self.student_data
                }, f)
        except Exception as e:
            print(f"Error saving model: {str(e)}", file=sys.stderr)
    
    def _load_model(self):
        """Load trained model"""
        try:
            if os.path.exists(self.encodings_path) and os.path.exists(self.labels_path):
                with open(self.encodings_path, 'rb') as f:
                    self.known_encodings = pickle.load(f)
                
                with open(self.labels_path, 'rb') as f:
                    data = pickle.load(f)
                    self.known_labels = data.get('labels', [])
                    self.student_data = data.get('student_data', {})
                return True
            return False
        except Exception as e:
            print(f"Error loading model: {str(e)}", file=sys.stderr)
            return False


def main():
    """Command-line interface for testing"""
    if len(sys.argv) < 2:
        print("Usage: python face_recognition_service.py <command> [args]", file=sys.stderr)
        print("Commands: train, recognize", file=sys.stderr)
        sys.exit(1)
    
    command = sys.argv[1]
    service = FaceRecognitionService()
    
    try:
        if command == "train":
            if len(sys.argv) < 3:
                print("Usage: python face_recognition_service.py train <students_json>", file=sys.stderr)
                sys.exit(1)
            
            students_json = sys.argv[2]
            with open(students_json, 'r', encoding='utf-8') as f:
                students_data = json.load(f)
            
            result = service.train_from_database(students_data)
            # Only print JSON to stdout - no other output
            print(json.dumps(result, ensure_ascii=False))
            
        elif command == "recognize":
            if len(sys.argv) < 3:
                print("Usage: python face_recognition_service.py recognize <image_path>", file=sys.stderr)
                sys.exit(1)
            
            image_path = sys.argv[2]
            result = service.recognize_face(image_path)
            # Only print JSON to stdout - no other output
            print(json.dumps(result, ensure_ascii=False))
            
        else:
            print(f"Unknown command: {command}", file=sys.stderr)
            sys.exit(1)
            
    except Exception as e:
        error_result = {
            "success": False,
            "message": f"Error: {str(e)}"
        }
        print(json.dumps(error_result, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    main()

