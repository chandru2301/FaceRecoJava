package com.fr.attendance.controller;

import com.fr.attendance.dto.StudentRegistrationDto;
import com.fr.attendance.entity.Student;
import com.fr.attendance.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for student registration and management.
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;

    /**
     * Registers a new student with face image.
     * 
     * @param dto Registration data (name, department, image)
     * @return Registered student
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerStudent(
            @Validated @ModelAttribute StudentRegistrationDto dto) {
        Map<String, Object> response = new HashMap<>();

        try {
            Student student = studentService.registerStudent(dto);
            response.put("success", true);
            response.put("message", "Student registered successfully");
            response.put("student", student);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            log.error("Error saving student image: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to save student image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Gets all registered students.
     * 
     * @return List of all students
     */
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    /**
     * Gets a student by name.
     * 
     * @param name The student's name
     * @return Student if found
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Map<String, Object>> getStudentByName(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        return studentService.getStudentByName(name)
                .map(student -> {
                    response.put("success", true);
                    response.put("student", student);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "Student not found with name: " + name);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Deletes a student.
     * 
     * @param id The student ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            studentService.deleteStudent(id);
            response.put("success", true);
            response.put("message", "Student deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            log.error("Error deleting student: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to delete student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
