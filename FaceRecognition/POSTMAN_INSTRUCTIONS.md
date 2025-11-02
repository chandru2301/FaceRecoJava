# Postman Configuration for Student Registration

## Using Postman with File Upload

Since Postman cannot directly capture from webcam, you have two options:

### Option 1: Upload Image File (Postman)

1. **Method**: `POST`
2. **URL**: `http://localhost:8080/api/students/register`
3. **Headers**: 
   - No Content-Type header needed (Postman sets it automatically for form-data)

4. **Body**:
   - Select `form-data`
   - Add three fields:

| Key | Type | Value |
|-----|------|-------|
| name | Text | John Doe |
| department | Text | Computer Science |
| image | File | Select a file (face.jpg) |

5. **Send the request**

**Note**: For webcam capture, use the web interface at `http://localhost:8080/register.html` (see below)

---

## Option 2: Using Web Interface (Recommended for Webcam)

A web interface is available at: `http://localhost:8080/register.html`

This allows you to:
- Capture image directly from webcam
- Enter student name and department
- Submit registration

---

## Postman Request Export (JSON)

```json
{
  "info": {
    "name": "Register Student",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Register Student",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "formdata",
          "formdata": [
            {
              "key": "name",
              "value": "John Doe",
              "type": "text"
            },
            {
              "key": "department",
              "value": "Computer Science",
              "type": "text"
            },
            {
              "key": "image",
              "type": "file",
              "src": []
            }
          ]
        },
        "url": {
          "raw": "http://localhost:8080/api/students/register",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "students", "register"]
        }
      }
    }
  ]
}
```

---

## Screenshot Instructions for Postman

1. **Create New Request**
   - Click "New" → "HTTP Request"

2. **Set Method and URL**
   - Method: `POST`
   - URL: `http://localhost:8080/api/students/register`

3. **Configure Body**
   - Click "Body" tab
   - Select "form-data"
   - Add fields:
     - `name` (Text): Enter student name
     - `department` (Text): Enter department
     - `image` (File): Click "Select Files" and choose an image

4. **Send Request**
   - Click "Send"
   - Check response for success/error

---

## Example cURL (for reference)

```bash
curl -X POST http://localhost:8080/api/students/register \
  -F "name=John Doe" \
  -F "department=Computer Science" \
  -F "image=@/path/to/face.jpg"
```

