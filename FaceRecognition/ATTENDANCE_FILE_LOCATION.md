# Attendance Excel File Location

## File Path

The attendance file `attendance.xlsx` is saved in the **current working directory** where your Spring Boot application runs from.

### Default Location

By default, this is typically:
- **Project Root**: `D:\java\FaceRecognition\attendance.xlsx`
- Or wherever you run `mvn spring-boot:run` from

### How to Find the File

1. **Check the project root directory** (where `pom.xml` is located)
2. The file will be created automatically when the first attendance is marked
3. If running from IDE (IntelliJ/Eclipse), check the project root directory

### Verify File Location

**Option 1: Check logs**
When attendance is marked, you'll see:
```
✅ chan marked as Present
```
The file is saved in the current working directory.

**Option 2: Use absolute path (recommended)**
I can update the code to save in a fixed location like:
- `D:\java\FaceRecognition\attendance.xlsx` (project root)
- `D:\java\FaceRecognition\data\attendance.xlsx` (data folder)
- Or any custom path you specify

### File Structure

The Excel file contains the following columns:

| Name | Department | Date | Status |
|------|------------|------|--------|
| chan | cs | 2025-11-01 | Present |
| John Doe | Computer Science | 2025-11-01 | Present |

### Notes

- The file is created automatically on first attendance mark
- If the file doesn't exist, it will be created
- Each person is marked only once per day (duplicate prevention)
- The file is updated in real-time as faces are recognized

### Change File Location

If you want to change where the file is saved, you can:

1. **Set in application.properties:**
```properties
attendance.excel.path=D:/java/FaceRecognition/data/attendance.xlsx
```

2. **Or modify `ExcelService.java`** to use a fixed absolute path

Let me know if you'd like me to update the code to use a specific location!

