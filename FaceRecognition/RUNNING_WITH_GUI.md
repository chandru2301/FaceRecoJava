# Running Face Recognition with GUI Window

## Problem
If the webcam light is on but no GUI window appears, Spring Boot might be running in headless mode.

## Solution

### Option 1: Run with JVM Argument (Recommended)

When starting the application, add `-Djava.awt.headless=false`:

**Using Maven:**
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.awt.headless=false"
```

**Using Java directly:**
```bash
java -Djava.awt.headless=false -jar target/face-recognition-*.jar
```

**Using IDE (IntelliJ/Eclipse):**
1. Go to Run Configuration
2. Add VM Options: `-Djava.awt.headless=false`
3. Or add to Program Arguments: `-Djava.awt.headless=false`

### Option 2: Set Environment Variable

```bash
# Windows (PowerShell)
$env:JAVA_TOOL_OPTIONS="-Djava.awt.headless=false"
mvn spring-boot:run

# Linux/Mac
export JAVA_TOOL_OPTIONS="-Djava.awt.headless=false"
mvn spring-boot:run
```

### Option 3: Create a Startup Script

**Windows (run-with-gui.bat):**
```batch
@echo off
set JAVA_TOOL_OPTIONS=-Djava.awt.headless=false
mvn spring-boot:run
```

**Linux/Mac (run-with-gui.sh):**
```bash
#!/bin/bash
export JAVA_TOOL_OPTIONS="-Djava.awt.headless=false"
mvn spring-boot:run
```

## Verification

After starting with GUI enabled, you should see:
- A window titled "Real-Time Face Recognition" appears
- Webcam feed is visible in the window
- Detected faces show green rectangles with names

If still no window appears, check the logs for:
```
✅ GUI window opened for face recognition display
```

## Headless Mode (Server Deployment)

If you're deploying to a server without a display, the system will:
- Still process face recognition
- Mark attendance in Excel
- Log all recognition events
- Work without a GUI window

In headless mode, check logs for messages like:
```
Processing frames... (processed X frames in last 5 seconds)
✅ chan (cs) marked Present in Excel
```

