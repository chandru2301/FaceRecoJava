# Step-by-Step API Access Guide

## Complete Workflow: From Registration to Attendance Tracking

---

## 📋 Prerequisites

1. **Start Spring Boot Application**
   ```bash
   mvn spring-boot:run
   ```
   Or run from your IDE.

2. **Verify Server is Running**
   ```bash
   curl http://localhost:8080/api/training/python-status
   ```
   Should return a JSON response.

---

## 🚀 Complete Step-by-Step Process

### **STEP 1: Register Students**

Register each student with their face image.

#### Option A: Using Webcam (Recommended)

1. **Open Registration Page:**
   ```
   http://localhost:8080/register.html
   ```

2. **Allow Camera Access** when prompted

3. **Fill in Details:**
   - Enter Student Name
   - Enter Department
   - Click "Capture Photo"
   - Click "Register Student"

#### Option B: Using API with cURL

```bash
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=Computer Science" \
  -F "image=@john.jpg"
```

**Expected Response:**
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

**Register More Students:**
```bash
curl -X POST http://localhost:8080/api/students/register \
  -F "name=Alice Smith" \
  -F "department=Electrical Engineering" \
  -F "image=@alice.jpg"

curl -X POST http://localhost:8080/api/students/register \
  -F "name=Bob Johnson" \
  -F "department=Mechanical Engineering" \
  -F "image=@bob.jpg"
```

---

### **STEP 2: Verify Registered Students**

Check all registered students:

```bash
curl http://localhost:8080/api/students
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "department": "Computer Science",
    "imagePath": "student_images/John_Doe_1234567890.jpg",
    "labelId": 0,
    "registeredAt": "2025-01-15T10:30:00"
  },
  {
    "id": 2,
    "name": "Alice Smith",
    "department": "Electrical Engineering",
    "imagePath": "student_images/Alice_Smith_1234567891.jpg",
    "labelId": 1,
    "registeredAt": "2025-01-15T10:35:00"
  }
]
```

---

### **STEP 3: Train the Face Recognition Model**

Train the model using registered students.

#### Option A: Auto-Detect (Python/Java)

```bash
curl -X POST http://localhost:8080/api/training/train
```

#### Option B: Check Python Status First

```bash
# Check if Python is available
curl http://localhost:8080/api/training/python-status
```

**Response (Python Available):**
```json
{
  "pythonAvailable": true,
  "message": "Python is available and ready to use"
}
```

**Response (Python Not Available):**
```json
{
  "pythonAvailable": false,
  "message": "Python is not available. Using Java implementation."
}
```

#### Option C: Force Implementation

```bash
# Force Python
curl -X POST "http://localhost:8080/api/training/train?usePython=python"

# Force Java
curl -X POST "http://localhost:8080/api/training/train?usePython=java"
```

**Expected Success Response:**
```json
{
  "success": true,
  "message": "Trained 2 faces successfully",
  "trained_count": 2
}
```

**If No Students Registered:**
```json
{
  "success": false,
  "message": "No students found in database. Please register students first."
}
```

---

### **STEP 4: Start Face Recognition**

Start the real-time face recognition and attendance marking.

```bash
curl -X POST http://localhost:8080/api/recognition/start
```

**Expected Success Response:**
```json
{
  "success": true,
  "running": true,
  "message": "Face recognition started successfully"
}
```

**If Model Not Trained:**
```json
{
  "success": false,
  "running": false,
  "message": "Failed to start face recognition. Please ensure: 1) Model is trained, 2) Webcam is connected, 3) Model file exists."
}
```

**What Happens:**
- ✅ Webcam window opens automatically
- ✅ Real-time face detection starts
- ✅ Recognized faces are automatically marked in Excel
- ✅ Name and confidence displayed on screen

---

### **STEP 5: Check Recognition Status**

Verify that recognition is running:

```bash
curl http://localhost:8080/api/recognition/status
```

**Response (Running):**
```json
{
  "running": true,
  "message": "Face recognition is active"
}
```

**Response (Stopped):**
```json
{
  "running": false,
  "message": "Face recognition is inactive"
}
```

---

### **STEP 6: Stop Recognition**

When done, stop the recognition process:

```bash
curl -X POST http://localhost:8080/api/recognition/stop
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Face recognition stopped successfully",
  "running": false
}
```

---

### **STEP 7: Check Attendance Records**

Open the generated Excel file:
```
attendance.xlsx
```

**Format:**
| Name | Department | Date | Status |
|------|------------|------|--------|
| John Doe | Computer Science | 2025-01-15 | Present |
| Alice Smith | Electrical Engineering | 2025-01-15 | Present |

---

## 🔄 Complete Example Workflow

### Quick Start Script

```bash
# 1. Register Students
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=CS" \
  -F "image=@john.jpg"

curl -X POST http://localhost:8080/api/students/register \
  -F "name=Alice Smith" \
  -F "department=EE" \
  -F "image=@alice.jpg"

# 2. Verify Registration
curl http://localhost:8080/api/students

# 3. Train Model
curl -X POST http://localhost:8080/api/training/train

# 4. Start Recognition
curl -X POST http://localhost:8080/api/recognition/start

# 5. Check Status
curl http://localhost:8080/api/recognition/status

# 6. Stop Recognition (when done)
curl -X POST http://localhost:8080/api/recognition/stop
```

---

## 📱 Using Postman Collection

### 1. Create Collection
- Name: "Face Recognition API"
- Base URL: `http://localhost:8080`

### 2. Create Requests

#### Request 1: Register Student
- **Method:** POST
- **URL:** `{{baseUrl}}/api/students/register`
- **Body:** form-data
  - `name`: Text → `John Doe`
  - `department`: Text → `Computer Science`
  - `image`: File → Select image file

#### Request 2: Get All Students
- **Method:** GET
- **URL:** `{{baseUrl}}/api/students`

#### Request 3: Train Model
- **Method:** POST
- **URL:** `{{baseUrl}}/api/training/train`

#### Request 4: Start Recognition
- **Method:** POST
- **URL:** `{{baseUrl}}/api/recognition/start`

#### Request 5: Check Status
- **Method:** GET
- **URL:** `{{baseUrl}}/api/recognition/status`

#### Request 6: Stop Recognition
- **Method:** POST
- **URL:** `{{baseUrl}}/api/recognition/stop`

---

## 🐛 Troubleshooting Steps

### Issue: Recognition starts but `running` is false

**Solution:**
1. Check if model is trained:
   ```bash
   ls trained_model.yml
   ```

2. If missing, train the model:
   ```bash
   curl -X POST http://localhost:8080/api/training/train
   ```

3. Check application logs for errors

### Issue: "Model file not found"

**Solution:**
```bash
# Train the model first
curl -X POST http://localhost:8080/api/training/train
```

### Issue: Webcam not working

**Check:**
- Webcam is connected
- No other application is using the webcam
- Camera permissions are granted

### Issue: "No students found"

**Solution:**
```bash
# Register students first
curl -X POST http://localhost:8080/api/students/register \
  -F "name=Test Student" \
  -F "department=Test Dept" \
  -F "image=@test.jpg"
```

---

## ✅ Verification Checklist

After each step, verify:

- [ ] **Step 1:** Student registered successfully (check response)
- [ ] **Step 2:** Students visible in GET response
- [ ] **Step 3:** Training completed (check response message)
- [ ] **Step 4:** Recognition started (`running: true`)
- [ ] **Step 5:** Webcam window opened
- [ ] **Step 6:** Faces detected (check webcam window)
- [ ] **Step 7:** Attendance marked (check `attendance.xlsx`)

---

## 📊 Status Codes Reference

| Status Code | Meaning |
|-------------|---------|
| 200 OK | Request successful |
| 201 Created | Resource created successfully |
| 400 Bad Request | Invalid request or already running/stopped |
| 404 Not Found | Resource not found |
| 500 Internal Server Error | Server error (check logs) |

---

## 🎯 Quick Command Reference

```bash
# Register Student
POST /api/students/register

# Get All Students
GET /api/students

# Train Model
POST /api/training/train

# Check Python Status
GET /api/training/python-status

# Start Recognition
POST /api/recognition/start

# Check Status
GET /api/recognition/status

# Stop Recognition
POST /api/recognition/stop
```

---

## 💡 Tips

1. **Always train after registering new students**
2. **Check logs for detailed error messages**
3. **Use webcam registration for better face capture**
4. **Wait for webcam window to fully load before expecting recognition**
5. **Each student can only be marked once per day (duplicate prevention)**

---

## 📝 Example Session Log

```bash
# Session Start
$ curl http://localhost:8080/api/students
[]

# Register First Student
$ curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" -F "department=CS" -F "image=@john.jpg"
{"success":true,"message":"Student registered successfully",...}

# Register Second Student
$ curl -X POST http://localhost:8080/api/students/register \
  -F "name=Alice" -F "department=EE" -F "image=@alice.jpg"
{"success":true,"message":"Student registered successfully",...}

# Train Model
$ curl -X POST http://localhost:8080/api/training/train
{"success":true,"message":"Trained 2 faces successfully","trained_count":2}

# Start Recognition
$ curl -X POST http://localhost:8080/api/recognition/start
{"success":true,"running":true,"message":"Face recognition started successfully"}

# Check Status
$ curl http://localhost:8080/api/recognition/status
{"running":true,"message":"Face recognition is active"}

# Stop Recognition
$ curl -X POST http://localhost:8080/api/recognition/stop
{"success":true,"message":"Face recognition stopped successfully","running":false}
```

