package com.fr.attendance.service;

import com.fr.attendance.dto.StudentRegistrationDto;
import com.fr.attendance.entity.Student;
import com.fr.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing student registration and face image storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String UPLOAD_DIR = "student_images";
    private final StudentRepository studentRepository;

    /**
     * Registers a new student with their face image.
     * 
     * @param dto The registration data
     * @return The registered student
     * @throws IOException If image saving fails
     */
    @Transactional
    public Student registerStudent(StudentRegistrationDto dto) throws IOException {
        // Validate required fields
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Student name is required");
        }
        
        if (dto.getDepartment() == null || dto.getDepartment().trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required");
        }
        
        if (dto.getImage() == null || dto.getImage().isEmpty()) {
            throw new IllegalArgumentException("Student image is required");
        }

        String name = dto.getName().trim();
        String department = dto.getDepartment().trim();

        // Check if student already exists
        if (studentRepository.existsByName(name)) {
            throw new IllegalArgumentException("Student with name '" + name + "' already exists");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename (sanitize name)
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = sanitizedName + "_" + System.currentTimeMillis() + ".jpg";
        Path filePath = uploadPath.resolve(fileName);

        // Save image file
        Files.copy(dto.getImage().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Saved student image: {}", filePath);

        // Assign label ID (next available)
        Integer nextLabelId = getNextLabelId();

        // Create and save student
        Student student = new Student();
        student.setName(name);
        student.setDepartment(department);
        student.setImagePath(filePath.toString());
        student.setLabelId(nextLabelId);

        Student savedStudent = studentRepository.save(student);
        log.info("Registered student: {} with label ID: {}", savedStudent.getName(), savedStudent.getLabelId());

        return savedStudent;
    }

    /**
     * Gets the next available label ID.
     * 
     * @return Next label ID
     */
    private Integer getNextLabelId() {
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            return 0;
        }
        return students.stream()
                .mapToInt(Student::getLabelId)
                .max()
                .orElse(-1) + 1;
    }

    /**
     * Gets all registered students.
     * 
     * @return List of all students
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Gets a student by name.
     * 
     * @param name The student's name
     * @return Optional student
     */
    public Optional<Student> getStudentByName(String name) {
        return studentRepository.findByName(name);
    }

    /**
     * Gets a student by label ID.
     * 
     * @param labelId The label ID
     * @return Optional student
     */
    public Optional<Student> getStudentByLabelId(Integer labelId) {
        return studentRepository.findByLabelId(labelId);
    }

    /**
     * Deletes a student.
     * 
     * @param id The student ID
     * @throws IOException If image deletion fails
     */
    @Transactional
    public void deleteStudent(Long id) throws IOException {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));

        // Delete image file
        Path imagePath = Paths.get(student.getImagePath());
        if (Files.exists(imagePath)) {
            Files.delete(imagePath);
            log.info("Deleted student image: {}", imagePath);
        }

        studentRepository.delete(student);
        log.info("Deleted student: {}", student.getName());
    }
}
