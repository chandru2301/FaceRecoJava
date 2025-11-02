# Python Setup Guide

## Issue: OpenCV Face Module Not Available

The error indicates that `cv2.face` module is not available. This module is part of `opencv-contrib-python`, not the standard `opencv-python`.

## Quick Fix

### Option 1: Install opencv-contrib-python (Recommended for OpenCV)

```bash
pip uninstall opencv-python
pip install opencv-contrib-python==4.8.1.78
```

### Option 2: Install face-recognition Library (Better accuracy)

```bash
pip install face-recognition
```

This library provides better accuracy and is easier to install on Windows.

### Option 3: Use Java Implementation

If Python libraries are causing issues, use the Java implementation:

```bash
curl -X POST "http://localhost:8080/api/training/train?usePython=java"
```

## Complete Installation

```bash
cd python
pip install -r requirements.txt
```

**Note:** On Windows, you may need to:
1. Install Visual C++ Build Tools for `dlib` (required by face-recognition)
2. Or use the pre-built wheel: `pip install dlib-binary`

## Verify Installation

```bash
# Test face_recognition
python -c "import face_recognition; print('face_recognition OK')"

# Test OpenCV face module
python -c "from cv2 import face; print('OpenCV face module OK')"
```

## Recommended Setup

For best results:
```bash
pip install face-recognition
```

This provides:
- ✅ Better accuracy
- ✅ Easier installation (usually)
- ✅ Faster recognition

## Fallback

If Python setup is difficult, the system automatically falls back to Java (OpenCV) implementation which is always available.

