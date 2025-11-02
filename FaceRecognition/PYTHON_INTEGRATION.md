# Python + Spring Boot Face Recognition Integration

This project integrates Python face recognition with Spring Boot Java application, providing a hybrid solution that uses Python's superior face recognition capabilities while maintaining Java/Spring Boot architecture.

## Architecture

- **Spring Boot (Java)**: Handles web API, database, and business logic
- **Python**: Handles face recognition training and recognition
- **Integration**: Java calls Python scripts via ProcessBuilder

## Setup

### 1. Install Python Dependencies

```bash
cd python
pip install -r requirements.txt
```

### 2. Verify Python Installation

The application will auto-detect Python. Check status:
```bash
GET http://localhost:8080/api/training/python-status
```

### 3. Train Model

The system automatically uses Python if available, otherwise falls back to Java:

```bash
# Auto-detect (prefers Python if available)
POST http://localhost:8080/api/training/train

# Force Python
POST http://localhost:8080/api/training/train?usePython=python

# Force Java
POST http://localhost:8080/api/training/train?usePython=java
```

## Features

### Dual Implementation Support

1. **Python Implementation** (Preferred)
   - Uses `face_recognition` library (dlib-based)
   - Better accuracy
   - Faster recognition
   - Falls back to OpenCV if `face_recognition` unavailable

2. **Java Implementation** (Fallback)
   - Uses JavaCV/OpenCV
   - Always available
   - Good accuracy with LBPH

### Automatic Detection

The system automatically:
- Detects Python installation
- Uses Python if available
- Falls back to Java if Python unavailable
- Logs which implementation is used

## API Endpoints

### Training

**POST** `/api/training/train`
- Trains face recognition model
- Auto-selects Python or Java
- Returns training results

**GET** `/api/training/python-status`
- Checks Python availability
- Returns status information

## Python Scripts

### `face_recognition_service.py`
- Main face recognition service
- Handles training and recognition
- Supports both `face_recognition` and OpenCV

### `realtime_face_recognition.py`
- Real-time recognition helper
- Processes individual frames
- Returns JSON results

## File Structure

```
project-root/
├── python/
│   ├── face_recognition_service.py
│   ├── realtime_face_recognition.py
│   ├── requirements.txt
│   └── models/
│       ├── face_encodings.pkl
│       ├── face_recognizer.pkl
│       └── labels.pkl
└── src/main/java/
    └── com/fr/attendance/service/
        └── PythonFaceRecognitionService.java
```

## Usage

### Training via API

```bash
curl -X POST http://localhost:8080/api/training/train
```

Response:
```json
{
  "success": true,
  "message": "Trained 5 faces successfully",
  "trained_count": 5
}
```

### Training via Python Directly

```bash
python python/face_recognition_service.py train students.json
```

### Recognition via Python Directly

```bash
python python/face_recognition_service.py recognize image.jpg
```

## Benefits

1. **Best of Both Worlds**: Python accuracy + Java architecture
2. **Fallback Support**: Always works even if Python unavailable
3. **Seamless Integration**: Java API, Python processing
4. **Performance**: Python face recognition is highly optimized

## Troubleshooting

### Python Not Found

If Python is not detected:
1. Install Python 3.7+
2. Add Python to PATH
3. Verify: `python --version`

### Missing Dependencies

Install required packages:
```bash
pip install opencv-python numpy face-recognition Pillow
```

### Permission Issues

Ensure Python scripts are executable:
```bash
chmod +x python/*.py
```

## Performance Comparison

| Implementation | Training Speed | Recognition Speed | Accuracy |
|---------------|----------------|-------------------|----------|
| Python (face_recognition) | Fast | Very Fast | Excellent |
| Python (OpenCV) | Medium | Fast | Good |
| Java (OpenCV) | Medium | Medium | Good |

## Notes

- Python models are stored separately from Java models
- Both implementations can coexist
- Training can switch between implementations
- Recognition can use either implementation

