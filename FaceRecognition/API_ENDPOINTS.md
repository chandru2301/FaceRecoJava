# Complete API Endpoints Reference

## Base URL
```
http://localhost:8080
```

---

## 🎓 Student Registration Endpoints

### 1. Register Student (with Webcam)
**POST** `/api/students/register`

Register a new student with name, department, and face image.

**Request (multipart/form-data):**
- `name` (String, required): Student name
- `department` (String, required): Department name
- `image` (File, required): Face image file (JPG/PNG)

**Example:**
```bash
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=Computer Science" \
  -F "image=@face.jpg"
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "Student registered successfully",
  "student": {
    "id": 1,
    "name": "John Doe",
    "department": "Computer Science",
    "imagePath": "student_images/John_Doe_1234567890.jpg",
    "labelId": 0,
    "registeredAt": "2025-01-15T10:30:00"
  }
}
```

**Web Interface:**
- URL: `http://localhost:8080/register.html`
- Features: Webcam capture, live preview

---

### 2. Get All Students
**GET** `/api/students`

Retrieve all registered students.

**Response (200):**
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "department": "Computer Science",
    "imagePath": "student_images/John_Doe_1234567890.jpg",
    "labelId": 0,
    "registeredAt": "2025-01-15T10:30:00"
  }
]
```

---

### 3. Get Student by Name
**GET** `/api/students/name/{name}`

Get a specific student by name.

**Example:**
```bash
curl http://localhost:8080/api/students/name/John%20Doe
```

**Response (200):**
```json
{
  "success": true,
  "student": {
    "id": 1,
    "name": "John Doe",
    "department": "Computer Science",
    "imagePath": "student_images/John_Doe_1234567890.jpg",
    "labelId": 0,
    "registeredAt": "2025-01-15T10:30:00"
  }
}
```

**Error (404):**
```json
{
  "success": false,
  "message": "Student not found with name: John Doe"
}
```

---

### 4. Delete Student
**DELETE** `/api/students/{id}`

Delete a student and their image file.

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/students/1
```

**Response (200):**
```json
{
  "success": true,
  "message": "Student deleted successfully"
}
```

---

## 🤖 Face Recognition Training Endpoints

### 5. Train Model
**POST** `/api/training/train`

Train the face recognition model from registered students.

**Query Parameters:**
- `usePython` (optional): 
  - `auto` (default): Auto-detect (prefers Python if available)
  - `python`: Force Python implementation
  - `java`: Force Java implementation

**Examples:**
```bash
# Auto-detect (prefers Python)
curl -X POST http://localhost:8080/api/training/train

# Force Python
curl -X POST "http://localhost:8080/api/training/train?usePython=python"

# Force Java
curl -X POST "http://localhost:8080/api/training/train?usePython=java"
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Trained 5 faces successfully",
  "trained_count": 5
}
```

**Error Response (500):**
```json
{
  "success": false,
  "message": "Failed to train model. Please ensure students are registered."
}
```

---

### 6. Check Python Status
**GET** `/api/training/python-status`

Check if Python is available for face recognition.

**Example:**
```bash
curl http://localhost:8080/api/training/python-status
```

**Response (200):**
```json
{
  "pythonAvailable": true,
  "message": "Python is available and ready to use"
}
```

or

```json
{
  "pythonAvailable": false,
  "message": "Python is not available. Using Java implementation."
}
```

---

## 📹 Real-Time Face Recognition Endpoints

### 7. Start Recognition
**POST** `/api/recognition/start`

Start real-time face recognition using webcam.

**Example:**
```bash
curl -X POST http://localhost:8080/api/recognition/start
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Face recognition started successfully",
  "running": true
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Face recognition is already running"
}
```

---

### 8. Stop Recognition
**POST** `/api/recognition/stop`

Stop the face recognition process.

**Example:**
```bash
curl -X POST http://localhost:8080/api/recognition/stop
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Face recognition stopped successfully",
  "running": false
}
```

---

### 9. Get Recognition Status
**GET** `/api/recognition/status`

Get the current status of face recognition.

**Example:**
```bash
curl http://localhost:8080/api/recognition/status
```

**Response (200):**
```json
{
  "running": true,
  "message": "Face recognition is active"
}
```

or

```json
{
  "running": false,
  "message": "Face recognition is inactive"
}
```

---

### 10. Recognize Faces in Image (Python)
**POST** `/api/recognition/recognize-image`

Recognize faces in an uploaded image using Python face recognition.

**Request:**
- `image` (multipart/form-data): Image file containing faces
- `usePython` (optional, default: "python"): Use Python implementation

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/recognition/recognize-image \
  -F "image=@photo.jpg" \
  -F "usePython=python"
```

**Example using Postman:**
1. Method: POST
2. URL: `http://localhost:8080/api/recognition/recognize-image`
3. Body → form-data:
   - Key: `image` (Type: File) → Select image file
   - Key: `usePython` (Type: Text) → Value: `python`

**Success Response (200):**
```json
{
  "success": true,
  "implementation": "python",
  "faces": [
    {
      "labelId": 0,
      "name": "John Doe",
      "department": "Computer Science",
      "confidence": 0.95,
      "location": [100, 200, 250, 150]
    }
  ]
}
```

**Error Response (400):**
```json
{
  "success": false,
  "message": "Python recognition not available. Please use Java implementation or install Python with face-recognition library."
}
```

**Notes:**
- This endpoint uses Python's `face_recognition` library for better accuracy
- Requires Python and face-recognition library to be installed
- Location format: `[top, right, bottom, left]` pixel coordinates
- Confidence score: 0.0 to 1.0 (higher is better)

---

## 📊 Web Interfaces

### 11. Home Page
**GET** `/` or `/index.html`

Main landing page with links.

**URL:** `http://localhost:8080/`

---

### 12. Student Registration Page
**GET** `/register.html`

Webcam-based student registration interface.

**URL:** `http://localhost:8080/register.html`

**Features:**
- Auto-start webcam
- Live face preview
- Capture photo
- Register student

---

## 📝 Complete Workflow Examples

### Workflow 1: Register and Train

```bash
# 1. Register students
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=CS" \
  -F "image=@john.jpg"

curl -X POST http://localhost:8080/api/students/register \
  -F "name=Alice Smith" \
  -F "department=EE" \
  -F "image=@alice.jpg"

# 2. Check Python status
curl http://localhost:8080/api/training/python-status

# 3. Train model (auto-detect)
curl -X POST http://localhost:8080/api/training/train

# 4. Start attendance
curl -X POST http://localhost:8080/api/recognition/start

# 5. Stop attendance
curl -X POST http://localhost:8080/api/recognition/stop
```

---

### Workflow 2: Using Web Interface

1. **Register Students:**
   - Open: `http://localhost:8080/register.html`
   - Allow camera access
   - Capture photo
   - Enter name and department
   - Click "Register Student"

2. **Train Model:**
   ```bash
   curl -X POST http://localhost:8080/api/training/train
   ```

3. **Start Attendance:**
   ```bash
   curl -X POST http://localhost:8080/api/recognition/start
   ```

4. **Check Status:**
   ```bash
   curl http://localhost:8080/api/recognition/status
   ```

---

## 🗂️ Excel Output

Attendance records are automatically saved to: `attendance.xlsx`

**File Location:**
The file is saved in the **current working directory** where Spring Boot runs (typically the project root: `D:\java\FaceRecognition\attendance.xlsx`).

### Get Attendance File Location
**GET** `/api/recognition/attendance-file`

Get the location and details of the attendance Excel file.

**Example:**
```bash
curl http://localhost:8080/api/recognition/attendance-file
```

**Response (200):**
```json
{
  "filePath": "D:\\java\\FaceRecognition\\attendance.xlsx",
  "exists": true,
  "absolutePath": "D:\\java\\FaceRecognition\\attendance.xlsx",
  "directory": "D:\\java\\FaceRecognition",
  "size": 10240,
  "lastModified": "2025-11-01T10:30:00.000Z"
}
```

**Format:**
| Name | Department | Date | Status |
|------|------------|------|--------|
| John Doe | Computer Science | 2025-01-15 | Present |
| Alice Smith | Electrical Engineering | 2025-01-15 | Present |

**Note:** The file is created automatically when the first attendance is marked.

---

## 📋 Quick Reference Table

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/students/register` | Register student with image |
| GET | `/api/students` | Get all students |
| GET | `/api/students/name/{name}` | Get student by name |
| DELETE | `/api/students/{id}` | Delete student |
| POST | `/api/training/train` | Train face recognition model |
| GET | `/api/training/python-status` | Check Python availability |
| POST | `/api/recognition/start` | Start face recognition |
| POST | `/api/recognition/stop` | Stop face recognition |
| GET | `/api/recognition/status` | Get recognition status |
| POST | `/api/recognition/recognize-image` | Recognize faces in uploaded image (Python) |
| GET | `/api/recognition/attendance-file` | Get attendance Excel file location |
| GET | `/register.html` | Webcam registration page |
| GET | `/` | Home page |

---

## 🔧 Error Responses

All endpoints return standard error responses:

**400 Bad Request:**
```json
{
  "success": false,
  "message": "Error description"
}
```

**404 Not Found:**
```json
{
  "success": false,
  "message": "Resource not found"
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "message": "Server error description"
}
```

---

## 📱 Using Postman

Import this collection or create requests manually:

### Collection Structure:
```
Face Recognition API
├── Students
│   ├── Register Student
│   ├── Get All Students
│   ├── Get Student by Name
│   └── Delete Student
├── Training
│   ├── Train Model
│   └── Check Python Status
└── Recognition
    ├── Start Recognition
    ├── Stop Recognition
    └── Get Status
```

---

## 🚀 Testing with cURL

```bash
# Get all students
curl http://localhost:8080/api/students

# Register student
curl -X POST http://localhost:8080/api/students/register \
  -F "name=Test Student" \
  -F "department=Test Dept" \
  -F "image=@test.jpg"

# Train model
curl -X POST http://localhost:8080/api/training/train

# Start recognition
curl -X POST http://localhost:8080/api/recognition/start

# Check status
curl http://localhost:8080/api/recognition/status

# Stop recognition
curl -X POST http://localhost:8080/api/recognition/stop
```

