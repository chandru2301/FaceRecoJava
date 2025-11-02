# Quick Fix for Training Error

## Problem
```
OpenCV training error: cannot import name 'face' from 'cv2'
```

## Solution

You have **3 options**:

### Option 1: Install face-recognition library (Recommended - Best Accuracy)

```bash
pip install face-recognition
```

This provides the best accuracy and is the preferred method.

### Option 2: Install opencv-contrib-python

```bash
pip uninstall opencv-python
pip install opencv-contrib-python==4.8.1.78
```

The standard `opencv-python` doesn't include the face module. You need `opencv-contrib-python`.

### Option 3: Use Java Implementation (Always Works)

Since you have the Java implementation working, use that instead:

```bash
curl -X POST "http://localhost:8080/api/training/train?usePython=java"
```

The Java implementation uses JavaCV which is already in your dependencies.

## Recommendation

For now, **use the Java implementation** since it's already working:

```bash
# This will work immediately
curl -X POST "http://localhost:8080/api/training/train?usePython=java"
```

Later, install `face-recognition` for better accuracy:
```bash
pip install face-recognition
```

Then Python will be used automatically.

