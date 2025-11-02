# Student Registration & Face Recognition Attendance System

## Overview

Complete system for registering students with face images and automatically marking attendance using real-time face recognition.

## Features

✅ **Student Registration**: Register students with name, department, and face image  
✅ **Database Storage**: All student data stored in H2 database  
✅ **Face Training**: Train recognition model from registered students  
✅ **Real-Time Recognition**: Automatic attendance marking using webcam  
✅ **Excel Export**: Attendance records with Name, Department, Date, and Status  

---

## API Endpoints

### Student Registration

#### 1. Register Student
**POST** `/api/students/register`

Register a new student with face image.

**Request (multipart/form-data):**
- `name`: Student name (required)
- `department`: Department name (required)
- `image`: Face image file (JPG/PNG, required)

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=Computer Science" \
  -F "image=@/path/to/face.jpg"
```

**Success Response (201 Created):**
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

#### 2. Get All Students
**GET** `/api/students`

Returns all registered students.

**Response:**
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

#### 3. Get Student by Name
**GET** `/api/students/name/{name}`

Get a specific student by name.

#### 4. Delete Student
**DELETE** `/api/students/{id}`

Delete a student and their image.

---

### Training

#### Train Model from Database
**POST** `/api/training/train`

Trains the face recognition model using all registered students from the database.

**Response:**
```json
{
  "success": true,
  "message": "Model trained successfully from database"
}
```

**⚠️ Important**: Train the model after registering students or when new students are added.

---

### Face Recognition & Attendance

#### Start Recognition
**POST** `/api/recognition/start`

Starts real-time face recognition and attendance marking.

#### Stop Recognition
**POST** `/api/recognition/stop`

Stops the face recognition process.

#### Check Status
**GET** `/api/recognition/status`

Get current recognition status.

---

## Workflow

### 1. Register Students

Register each student with their face image:

```bash
# Register student 1
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=Computer Science" \
  -F "image=@john.jpg"

# Register student 2
curl -X POST http://localhost:8080/api/students/register \
  -F "name=Alice Smith" \
  -F "department=Electrical Engineering" \
  -F "image=@alice.jpg"
```

### 2. Train the Model

After registering all students, train the recognition model:

```bash
curl -X POST http://localhost:8080/api/training/train
```

This will:
- Load all student images from the database
- Detect faces in each image
- Train the LBPH recognizer
- Save the model to `trained_model.yml`

### 3. Start Attendance

Start real-time face recognition:

```bash
curl -X POST http://localhost:8080/api/recognition/start
```

The system will:
- Open webcam window
- Detect and recognize faces in real-time
- Automatically mark attendance in `attendance.xlsx`
- Display name and department on screen

### 4. Stop Recognition

```bash
curl -X POST http://localhost:8080/api/recognition/stop
```

---

## Excel Attendance Output

The system creates/updates `attendance.xlsx` with the following structure:

| Name | Department | Date | Status |
|------|------------|------|--------|
| John Doe | Computer Science | 2025-01-15 | Present |
| Alice Smith | Electrical Engineering | 2025-01-15 | Present |

---

## Database Schema

### Students Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-generated) |
| name | VARCHAR | Student name (unique) |
| department | VARCHAR | Department name |
| image_path | VARCHAR(1000) | Path to face image file |
| label_id | INTEGER | Label ID assigned during training |
| registered_at | TIMESTAMP | Registration timestamp |

---

## Image Requirements

- **Format**: JPG, JPEG, PNG, or BMP
- **Quality**: Clear frontal face images
- **Lighting**: Good, consistent lighting
- **Resolution**: Minimum 200x200 pixels recommended
- **Face Visibility**: Face should be clearly visible and centered

---

## File Structure

```
project-root/
├── student_images/          # Stored student face images
│   ├── John_Doe_1234567890.jpg
│   └── Alice_Smith_1234567891.jpg
├── trained_model.yml       # Trained recognition model
├── label_names.txt         # Label-to-name mapping (legacy)
└── attendance.xlsx         # Attendance records
```

---

## Example: Complete Workflow

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

# 2. Train model
curl -X POST http://localhost:8080/api/training/train

# 3. Start attendance
curl -X POST http://localhost:8080/api/recognition/start

# ... Recognition running ...

# 4. Stop attendance
curl -X POST http://localhost:8080/api/recognition/stop

# 5. Check attendance file
# Open attendance.xlsx to see marked attendance
```

---

## Notes

- **Train After Registration**: Always train the model after registering new students
- **Image Storage**: Images are saved in `student_images/` directory
- **Database**: Uses H2 in-memory database (data persists during application run)
- **Duplicate Prevention**: Each student can only be marked once per day
- **Model File**: `trained_model.yml` is created/updated during training

---

## Troubleshooting

### "No students found in database"
- Register students first using `/api/students/register`
- Then train using `/api/training/train`

### "No face detected in image"
- Ensure the image contains a clear frontal face
- Check image quality and lighting
- Try a different image

### "Student already exists"
- Student names must be unique
- Use a different name or delete existing student first

---

## Security Considerations

- Add authentication/authorization for production use
- Validate and sanitize image uploads
- Implement file size limits
- Add rate limiting for API endpoints

