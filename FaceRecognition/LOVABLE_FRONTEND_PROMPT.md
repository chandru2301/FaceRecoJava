# Lovable Frontend Prompt: Face Recognition Attendance System

## Project Overview
Create a modern, responsive web application frontend for a Face Recognition Attendance System. The backend is a Spring Boot REST API that handles student registration, face recognition training, and real-time attendance marking via webcam.

## Backend API Base URL
```
http://localhost:8080
```

All API endpoints support CORS from any origin (`*`).

---

## Required Features & Pages

### 1. Dashboard/Home Page (`/`)
- **Purpose**: Central hub with quick stats and navigation
- **Components**:
  - Overview cards showing:
    - Total registered students
    - Recognition status (running/stopped)
    - Today's attendance count
    - System status indicators
  - Quick action buttons:
    - Start/Stop Recognition
    - Train Model
    - Register New Student
  - Recent activity feed
  - Link to attendance file location

### 2. Student Registration Page (`/register`)
- **Purpose**: Register new students with webcam photo capture
- **Features**:
  - Real-time webcam feed using browser `getUserMedia()` API
  - Live face preview with visual feedback
  - Capture photo button
  - Form fields:
    - Name (required, text input)
    - Department (required, text input)
    - Image preview of captured photo
  - Submit button that sends multipart/form-data
  - Success/error notifications
  - Validation feedback for empty fields
- **API**: `POST /api/students/register`
- **Request Format**: `multipart/form-data` with:
  - `name`: string
  - `department`: string
  - `image`: File (JPG/PNG)

### 3. Students Management Page (`/students`)
- **Purpose**: View, search, and manage registered students
- **Features**:
  - Table/list view of all students showing:
    - ID
    - Name
    - Department
    - Registration Date
    - Profile image thumbnail
  - Search/filter functionality
  - Delete button for each student (with confirmation dialog)
  - "Register New Student" button linking to `/register`
  - Loading states and empty state messages
- **API**: `GET /api/students`, `DELETE /api/students/{id}`

### 4. Training Page (`/training`)
- **Purpose**: Train the face recognition model
- **Features**:
  - Display total registered students count
  - Python availability status indicator
  - Training method selector (Auto/Python/Java)
  - "Train Model" button with loading state
  - Progress indicator during training
  - Success/error notifications with training results
  - Training history/log
- **API**: `GET /api/training/python-status`, `POST /api/training/train?usePython={auto|python|java}`

### 5. Real-Time Recognition Page (`/recognition`)
- **Purpose**: Start/stop face recognition and monitor attendance
- **Features**:
  - Large status indicator (Running/Stopped)
  - Start/Stop buttons (disable start when running, disable stop when stopped)
  - Real-time status polling (every 2-3 seconds)
  - Live recognition feed display area (showing recognized faces)
  - Attendance log section showing:
    - Recently recognized students
    - Time of recognition
    - Department
  - "View Attendance File" button linking to Excel file location
  - Connection status indicator
- **API**: 
  - `GET /api/recognition/status`
  - `POST /api/recognition/start`
  - `POST /api/recognition/stop`
  - `GET /api/recognition/attendance-file`

### 6. Image Recognition Page (`/recognize-image`)
- **Purpose**: Upload an image to recognize faces (optional feature)
- **Features**:
  - File upload component for image
  - Image preview after selection
  - "Recognize" button
  - Results display showing:
    - Detected faces with bounding boxes
    - Recognized names and departments
    - Confidence scores
  - Loading state during processing
- **API**: `POST /api/recognition/recognize-image` (multipart/form-data with `image` file)

---

## API Endpoints Reference

### Student Endpoints

#### Register Student
```
POST /api/students/register
Content-Type: multipart/form-data
Body: { name: string, department: string, image: File }

Success Response (201):
{
  "success": true,
  "message": "Student registered successfully",
  "student": {
    "id": number,
    "name": string,
    "department": string,
    "imagePath": string,
    "labelId": number,
    "registeredAt": string (ISO datetime)
  }
}

Error Response (400):
{
  "success": false,
  "message": "Error description"
}
```

#### Get All Students
```
GET /api/students

Response (200):
[
  {
    "id": number,
    "name": string,
    "department": string,
    "imagePath": string,
    "labelId": number,
    "registeredAt": string
  }
]
```

#### Delete Student
```
DELETE /api/students/{id}

Success Response (200):
{
  "success": true,
  "message": "Student deleted successfully"
}
```

### Training Endpoints

#### Train Model
```
POST /api/training/train?usePython={auto|python|java}

Success Response (200):
{
  "success": true,
  "message": "Trained 5 faces successfully",
  "trained_count": number,
  "implementation": "java" | "python"
}

Error Response (500):
{
  "success": false,
  "message": "Error description"
}
```

#### Check Python Status
```
GET /api/training/python-status

Response (200):
{
  "pythonAvailable": boolean,
  "message": string
}
```

### Recognition Endpoints

#### Start Recognition
```
POST /api/recognition/start

Success Response (200):
{
  "success": true,
  "message": "Face recognition started successfully",
  "running": true,
  "implementation": "java"
}

Error Response (400):
{
  "success": false,
  "message": "Face recognition is already running",
  "running": true
}
```

#### Stop Recognition
```
POST /api/recognition/stop

Success Response (200):
{
  "success": true,
  "message": "Face recognition stopped successfully",
  "running": false
}
```

#### Get Recognition Status
```
GET /api/recognition/status

Response (200):
{
  "running": boolean,
  "message": string
}
```

#### Get Attendance File Location
```
GET /api/recognition/attendance-file

Response (200):
{
  "filePath": string,
  "exists": boolean,
  "absolutePath": string,
  "directory": string,
  "size": number (if exists),
  "lastModified": string (if exists)
}
```

#### Recognize Image (Optional)
```
POST /api/recognition/recognize-image
Content-Type: multipart/form-data
Body: { image: File, usePython?: string }

Success Response (200):
{
  "success": true,
  "implementation": "python",
  "faces": [
    {
      "labelId": number,
      "name": string,
      "department": string,
      "confidence": number (0-1),
      "location": [number, number, number, number]
    }
  ]
}
```

---

## UI/UX Requirements

### Design Style
- **Modern and clean**: Use a professional, minimalist design
- **Color Scheme**: 
  - Primary: Blue/Indigo (#3B82F6 or similar)
  - Success: Green (#10B981)
  - Danger/Warning: Red (#EF4444)
  - Neutral: Gray scale
- **Typography**: Clean, readable sans-serif font (e.g., Inter, Roboto, or system fonts)
- **Responsive**: Mobile-first design that works on desktop, tablet, and mobile
- **Accessibility**: WCAG 2.1 AA compliant, proper ARIA labels, keyboard navigation

### Components Library
- Use a modern component library (e.g., shadcn/ui, Material-UI, Ant Design, or Tailwind UI)
- Consistent button styles, form inputs, cards, modals
- Loading spinners and skeleton screens
- Toast notifications for success/error messages

### Key UI Patterns
1. **Navigation**: Sidebar or top navigation bar with links to all pages
2. **Status Indicators**: Use color-coded badges/chips (green for active, red for inactive, yellow for pending)
3. **Loading States**: Show spinners or skeleton screens during API calls
4. **Error Handling**: User-friendly error messages with actionable suggestions
5. **Confirmation Dialogs**: For destructive actions (e.g., delete student)

---

## Technical Requirements

### Technology Stack (Recommended)
- **Framework**: React with TypeScript (or Angular if preferred)
- **State Management**: React Query/SWR for server state, Context API or Zustand for global state
- **HTTP Client**: Axios or Fetch API
- **Styling**: Tailwind CSS or styled-components
- **Form Handling**: React Hook Form or Formik
- **Date/Time**: date-fns or moment.js
- **Icons**: Lucide React, Heroicons, or similar

### Key Technical Features
1. **Real-time Updates**: 
   - Poll `/api/recognition/status` every 2-3 seconds when on recognition page
   - WebSocket support (if available in backend) or long polling
2. **Webcam Integration**: 
   - Use `navigator.mediaDevices.getUserMedia()` for camera access
   - Handle permissions gracefully
   - Show error messages if camera not available
3. **File Upload**: 
   - Support drag-and-drop for image uploads
   - Preview images before submission
   - Show file size/format validation
4. **Error Handling**: 
   - Global error handler for API failures
   - Retry logic for failed requests
   - Network error detection
5. **Performance**: 
   - Lazy load routes
   - Optimize image loading
   - Debounce search inputs
   - Cache API responses where appropriate

---

## User Workflow

### Typical Flow
1. **Register Students** → Go to `/register`, capture photo, fill details, submit
2. **Train Model** → Go to `/training`, click "Train Model", wait for completion
3. **Start Recognition** → Go to `/recognition`, click "Start", monitor live attendance
4. **View Students** → Go to `/students` to see all registered students
5. **Check Attendance** → Click "View Attendance File" to see Excel location

---

## Additional Features to Consider
- **Dark Mode**: Toggle between light/dark themes
- **Export Attendance**: Download attendance data as CSV/Excel
- **Statistics Dashboard**: Charts showing attendance trends
- **Student Profile View**: Detailed view of individual student with attendance history
- **Bulk Upload**: Upload multiple student images at once
- **Settings Page**: Configure recognition thresholds, camera settings

---

## Mock Data Structure
If backend is unavailable, use these mock responses:
```typescript
// Mock Student
{
  "id": 1,
  "name": "John Doe",
  "department": "Computer Science",
  "imagePath": "student_images/john_123.jpg",
  "labelId": 0,
  "registeredAt": "2025-01-15T10:30:00"
}

// Mock Recognition Status
{
  "running": true,
  "message": "Face recognition is active"
}

// Mock Training Response
{
  "success": true,
  "message": "Trained 5 faces successfully",
  "trained_count": 5,
  "implementation": "java"
}
```

---

## Notes for Implementation
- **CORS**: Backend already supports `*`, so no CORS issues expected
- **Authentication**: Currently none required (add auth UI if needed later)
- **Image URLs**: Student images are stored server-side; use full URLs or proxy through backend
- **Real-time Recognition**: The backend runs recognition in a separate thread; frontend polls for status
- **Excel File**: Attendance file is server-side; frontend shows location, doesn't display content directly

---

## Deliverables
1. Fully functional single-page application (SPA) or multi-page app with routing
2. All pages listed above implemented
3. Responsive design for mobile, tablet, desktop
4. Error handling and loading states throughout
5. Clean, maintainable code with TypeScript
6. README with setup instructions and API integration guide

---

## Questions or Clarifications?
- If the backend API format differs from what's documented, adapt accordingly
- Use environment variables for API base URL (default: `http://localhost:8080`)
- Handle edge cases gracefully (no students, no camera, network errors, etc.)

---

**Start building the frontend with a focus on user experience, clean code, and reliability!**

