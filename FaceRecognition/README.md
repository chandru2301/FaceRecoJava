# Real-Time Face Recognition Attendance System

A Spring Boot application that uses OpenCV and JavaCV for real-time face recognition and automatically marks attendance in an Excel file.

## Features

- ✅ Real-time face detection and recognition using webcam
- ✅ Automatic attendance marking in Excel (.xlsx) format
- ✅ LBPH (Local Binary Patterns Histograms) face recognition
- ✅ REST API for remote control
- ✅ Prevents duplicate entries (one mark per person per day)
- ✅ Visual feedback with name overlay on detected faces

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Webcam connected to your computer
- Training images (see Setup section)

## Project Structure

```
src/main/java/com/fr/
├── attendance/
│   ├── controller/
│   │   └── FaceRecognitionController.java    # REST endpoints
│   ├── model/
│   │   └── LabelNameMapper.java              # Maps labels to names
│   ├── service/
│   │   ├── ExcelService.java                 # Excel attendance management
│   │   ├── FaceRecognitionService.java       # High-level service
│   │   └── RealTimeFaceRecognition.java      # Core recognition logic
│   └── FaceTrainer.java                      # Training utility
├── resources/
│   └── haarcascade_frontalface_default.xml   # Face detection classifier
faces/                                        # Training images directory
├── person1/
│   ├── 1.jpg
│   ├── 2.jpg
│   └── ...
└── person2/
    ├── 1.jpg
    └── ...
```

## Setup Instructions

### 1. Prepare Training Images

Create a `faces/` directory in the project root and add subdirectories for each person:

```
faces/
├── john/
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   └── ...
└── alice/
    ├── 1.jpg
    ├── 2.jpg
    └── ...
```

**Tips for training images:**
- Use 5-10 clear frontal face images per person
- Ensure good lighting and minimal shadows
- Use consistent lighting across images
- Images should be in JPG, JPEG, PNG, or BMP format
- Avoid blurry or low-resolution images

### 2. Train the Face Recognition Model

Run the `FaceTrainer` class to train the model:

```bash
# Using Maven
mvn compile exec:java -Dexec.mainClass="com.fr.attendance.FaceTrainer"

# Or run directly in your IDE
```

This will:
- Process all images from the `faces/` directory
- Detect faces in each image
- Train the LBPH recognizer
- Save the model to `trained_model.yml`
- Save label-to-name mapping to `label_names.txt`

### 3. Build and Run the Application

```bash
# Build the project
mvn clean install

# Run the Spring Boot application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Usage

### Starting Face Recognition

**Option 1: Using REST API**

```bash
# Start recognition
curl -X POST http://localhost:8080/api/recognition/start

# Check status
curl http://localhost:8080/api/recognition/status

# Stop recognition
curl -X POST http://localhost:8080/api/recognition/stop
```

**Option 2: Programmatically**

You can inject `FaceRecognitionService` into your code:

```java
@Autowired
private FaceRecognitionService faceRecognitionService;

public void start() {
    faceRecognitionService.startRecognition();
}
```

### How It Works

1. When recognition starts, a window opens showing the webcam feed
2. The system detects faces in real-time
3. For each detected face:
   - It attempts to recognize the person
   - If confidence < 80 and the person is known, it marks attendance
   - The name and confidence are displayed on-screen
   - Attendance is written to `attendance.xlsx`

### Excel Output

The system creates/updates `attendance.xlsx` with the following structure:

| Name | Date       | Status  |
|------|------------|---------|
| John | 2025-01-15 | Present |
| Alice| 2025-01-15 | Present |

- **Duplicate Prevention**: Each person can only be marked once per day
- **Automatic Date**: Uses the current system date
- **Format**: Excel (.xlsx) format for easy opening in Excel/LibreOffice

## API Endpoints

### POST `/api/recognition/start`
Starts the face recognition process.

**Response:**
```json
{
  "success": true,
  "message": "Face recognition started successfully",
  "running": true
}
```

### POST `/api/recognition/stop`
Stops the face recognition process.

**Response:**
```json
{
  "success": true,
  "message": "Face recognition stopped successfully",
  "running": false
}
```

### GET `/api/recognition/status`
Gets the current status of face recognition.

**Response:**
```json
{
  "running": true,
  "message": "Face recognition is active"
}
```

## Configuration

### Adjusting Recognition Confidence

Edit `RealTimeFaceRecognition.java`:

```java
private static final double CONFIDENCE_THRESHOLD = 80.0; // Lower = more permissive
```

- Lower values (e.g., 70) = more matches but higher false positives
- Higher values (e.g., 90) = fewer matches but more accurate

### Changing File Paths

Edit constants in:
- `ExcelService.java`: `EXCEL_PATH = "attendance.xlsx"`
- `RealTimeFaceRecognition.java`: `MODEL_PATH = "trained_model.yml"`
- `FaceTrainer.java`: `TRAINING_DIR = "faces/"`

## Troubleshooting

### Webcam Not Detected
- Ensure your webcam is connected and not used by another application
- Try changing the camera index: `new OpenCVFrameGrabber(0)` → `new OpenCVFrameGrabber(1)`

### Poor Recognition Accuracy
- Add more training images (aim for 8-10 per person)
- Use clear, well-lit frontal face images
- Ensure faces are centered and not at extreme angles
- Adjust `CONFIDENCE_THRESHOLD` if needed

### Model Not Found
- Make sure you've run `FaceTrainer` first
- Check that `trained_model.yml` exists in the project root

### No Faces Detected
- Ensure `haarcascade_frontalface_default.xml` exists in `src/main/resources/`
- Check lighting conditions - face detection works better with good lighting
- Make sure faces are clearly visible and frontal

## Dependencies

- **Spring Boot 3.5.7**: Application framework
- **JavaCV 1.5.10**: OpenCV Java bindings (includes native libraries)
- **Apache POI 5.3.0**: Excel file manipulation
- **Lombok**: Reduces boilerplate code

## Notes

- The first run may take longer as JavaCV downloads native libraries
- On Windows, ensure Visual C++ Redistributables are installed
- The recognition window must remain open while recognition is active
- Closing the recognition window will stop the process

## License

This project is open source and available for educational purposes.
