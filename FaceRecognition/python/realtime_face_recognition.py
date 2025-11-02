"""
Real-time face recognition using webcam
Called from Java Spring Boot application
"""

import sys
import json
import cv2
import os
import pickle
import numpy as np

# Try to import face_recognition library
try:
    import face_recognition
    FACE_RECOGNITION_AVAILABLE = True
except ImportError:
    FACE_RECOGNITION_AVAILABLE = False

class RealTimeRecognition:
    def __init__(self, model_dir="python/models"):
        self.model_dir = model_dir
        self.known_encodings = []
        self.known_labels = []
        self.student_data = {}
        self.recognizer = None
        self.load_model()
    
    def load_model(self):
        """Load trained face recognition model"""
        encodings_path = os.path.join(self.model_dir, "face_encodings.pkl")
        labels_path = os.path.join(self.model_dir, "labels.pkl")
        model_path = os.path.join(self.model_dir, "face_recognizer.pkl")
        
        if FACE_RECOGNITION_AVAILABLE:
            if os.path.exists(encodings_path) and os.path.exists(labels_path):
                with open(encodings_path, 'rb') as f:
                    self.known_encodings = pickle.load(f)
                with open(labels_path, 'rb') as f:
                    data = pickle.load(f)
                    self.known_labels = data.get('labels', [])
                    self.student_data = data.get('student_data', {})
        else:
            if os.path.exists(model_path) and os.path.exists(labels_path):
                import cv2
                try:
                    from cv2 import face
                    self.recognizer = face.LBPHFaceRecognizer_create()
                    self.recognizer.read(model_path)
                    with open(labels_path, 'rb') as f:
                        self.student_data = pickle.load(f)
                except ImportError:
                    # OpenCV face module not available
                    pass
    
    def recognize_frame(self, frame_path):
        """
        Recognize faces in a frame
        Args:
            frame_path: Path to captured frame image
        Returns:
            JSON string with recognition results
        """
        try:
            if not os.path.exists(frame_path):
                return json.dumps({"success": False, "message": "Frame not found"})
            
            if FACE_RECOGNITION_AVAILABLE and self.known_encodings:
                return self._recognize_with_face_recognition(frame_path)
            elif self.recognizer:
                return self._recognize_with_opencv(frame_path)
            else:
                return json.dumps({"success": False, "message": "Model not loaded"})
                
        except Exception as e:
            return json.dumps({"success": False, "message": str(e)})
    
    def _recognize_with_face_recognition(self, frame_path):
        """Recognize using face_recognition library"""
        try:
            image = face_recognition.load_image_file(frame_path)
            face_locations = face_recognition.face_locations(image)
            face_encodings = face_recognition.face_encodings(image, face_locations)
            
            if len(face_encodings) == 0:
                return json.dumps({"success": True, "faces": []})
            
            results = []
            for face_encoding, face_location in zip(face_encodings, face_locations):
                matches = face_recognition.compare_faces(
                    self.known_encodings, 
                    face_encoding, 
                    tolerance=0.6
                )
                face_distances = face_recognition.face_distance(
                    self.known_encodings, 
                    face_encoding
                )
                
                best_match_index = np.argmin(face_distances)
                confidence = 1 - face_distances[best_match_index]
                
                if matches[best_match_index] and confidence > 0.5:
                    label_id = self.known_labels[best_match_index]
                    student = self.student_data.get(label_id, {})
                    
                    results.append({
                        "labelId": int(label_id),
                        "name": student.get('name', 'Unknown'),
                        "department": student.get('department', ''),
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
            
            return json.dumps({"success": True, "faces": results})
            
        except Exception as e:
            return json.dumps({"success": False, "message": str(e)})
    
    def _recognize_with_opencv(self, frame_path):
        """Recognize using OpenCV"""
        try:
            import cv2
            
            img = cv2.imread(frame_path)
            if img is None:
                return json.dumps({"success": False, "message": "Could not load image"})
            
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            
            # Detect faces
            cascade_paths = [
                os.path.join(self.model_dir, "haarcascade_frontalface_default.xml"),
                "src/main/resources/haarcascade_frontalface_default.xml",
                "../src/main/resources/haarcascade_frontalface_default.xml"
            ]
            
            face_cascade = None
            for path in cascade_paths:
                if os.path.exists(path):
                    face_cascade = cv2.CascadeClassifier(path)
                    break
            
            if face_cascade is None or face_cascade.empty():
                return json.dumps({"success": False, "message": "Cascade not found"})
            
            faces = face_cascade.detectMultiScale(gray, 1.1, 5)
            
            if len(faces) == 0:
                return json.dumps({"success": True, "faces": []})
            
            results = []
            for (x, y, w, h) in faces:
                face_roi = gray[y:y+h, x:x+w]
                face_roi = cv2.resize(face_roi, (200, 200))
                
                label_id, confidence = self.recognizer.predict(face_roi)
                confidence_score = (100 - confidence) / 100.0
                
                if confidence_score > 0.5 and label_id in self.student_data:
                    student = self.student_data[label_id]
                    results.append({
                        "labelId": int(label_id),
                        "name": student.get('name', 'Unknown'),
                        "department": student.get('department', ''),
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
            
            return json.dumps({"success": True, "faces": results})
            
        except Exception as e:
            return json.dumps({"success": False, "message": str(e)})


def main():
    """Command-line interface"""
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "message": "Usage: recognize <frame_path>"}))
        sys.exit(1)
    
    frame_path = sys.argv[1]
    recognizer = RealTimeRecognition()
    result = recognizer.recognize_frame(frame_path)
    print(result)


if __name__ == "__main__":
    main()

