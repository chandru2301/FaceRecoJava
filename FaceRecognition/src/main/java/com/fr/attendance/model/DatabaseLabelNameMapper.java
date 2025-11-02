package com.fr.attendance.model;

import com.fr.attendance.entity.Student;
import com.fr.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps label IDs to student names and departments from the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseLabelNameMapper {

    private final StudentRepository studentRepository;
    private Map<Integer, String> labelNameMap;
    private Map<Integer, String> labelDepartmentMap;

    /**
     * Loads the label-to-name and label-to-department mappings from the database.
     */
    public void loadMappings() {
        labelNameMap = new HashMap<>();
        labelDepartmentMap = new HashMap<>();

        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            labelNameMap.put(student.getLabelId(), student.getName());
            labelDepartmentMap.put(student.getLabelId(), student.getDepartment());
        }

        log.info("Loaded {} label mappings from database", labelNameMap.size());
    }

    /**
     * Gets the name for a given label ID.
     * 
     * @param label The label ID
     * @return The name, or "Unknown" if not found
     */
    public String getName(int label) {
        if (labelNameMap == null) {
            loadMappings();
        }
        return labelNameMap.getOrDefault(label, "Unknown");
    }

    /**
     * Gets the department for a given label ID.
     * 
     * @param label The label ID
     * @return The department, or empty string if not found
     */
    public String getDepartment(int label) {
        if (labelDepartmentMap == null) {
            loadMappings();
        }
        return labelDepartmentMap.getOrDefault(label, "");
    }

    /**
     * Gets the student for a given label ID.
     * 
     * @param label The label ID
     * @return Optional student
     */
    public Optional<Student> getStudent(int label) {
        return studentRepository.findByLabelId(label);
    }

    /**
     * Checks if a label ID exists in the mapping.
     * 
     * @param label The label ID
     * @return true if the label exists, false otherwise
     */
    public boolean containsLabel(int label) {
        if (labelNameMap == null) {
            loadMappings();
        }
        return labelNameMap.containsKey(label);
    }

    /**
     * Gets the number of registered students.
     * 
     * @return Number of registered students
     */
    public int getStudentCount() {
        if (labelNameMap == null) {
            loadMappings();
        }
        return labelNameMap.size();
    }
}
