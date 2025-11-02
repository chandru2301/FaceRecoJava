# Python Installation Guide - Troubleshooting

## Quick Solution: Use Java Implementation

**The easiest solution is to use the Java implementation which already works:**

```bash
curl -X POST "http://localhost:8080/api/training/train?usePython=java"
```

This doesn't require any Python packages and works immediately.

---

## If You Want to Use Python

### Problem: Numpy Installation Issues

The numpy version in requirements.txt may not be compatible with Python 3.12 or requires build tools.

### Solution 1: Install Compatible Versions

```bash
# Install numpy first (latest compatible version)
pip install numpy

# Then install face-recognition (it will handle its own dependencies)
pip install face-recognition

# Or install opencv-contrib-python (alternative)
pip install opencv-contrib-python
```

### Solution 2: Use Pre-built Wheels

```bash
# Uninstall if already installed
pip uninstall numpy opencv-python opencv-contrib-python -y

# Install latest compatible versions (no specific version constraints)
pip install numpy opencv-contrib-python face-recognition Pillow
```

### Solution 3: Install Only face-recognition (Recommended)

If you only want better accuracy, just install face-recognition:

```bash
pip install face-recognition
```

This will automatically install compatible versions of numpy and dlib.

---

## Recommended Minimal Setup

For best results with minimal setup issues:

```bash
# Just install face-recognition - it handles everything
pip install face-recognition

# Verify installation
python -c "import face_recognition; print('OK')"
```

Then training will automatically use Python if available.

---

## Current Workaround

Since you have Java working, continue using:

```bash
# Train with Java (always works)
curl -X POST "http://localhost:8080/api/training/train?usePython=java"

# Start recognition
curl -X POST http://localhost:8080/api/recognition/start
```

The Java implementation works perfectly and doesn't require Python setup.

---

## Alternative: Update requirements.txt

I've updated `requirements.txt` to use flexible versions. Try:

```bash
cd python
pip install -r requirements.txt
```

If it still fails, install individually:
```bash
pip install numpy
pip install opencv-contrib-python  
pip install face-recognition
pip install Pillow
```

