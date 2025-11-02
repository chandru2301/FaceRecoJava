package com.fr.attendance.repository;

import com.fr.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Student entity operations.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Finds a student by name.
     * 
     * @param name The student's name
     * @return Optional student
     */
    Optional<Student> findByName(String name);

    /**
     * Finds a student by label ID.
     * 
     * @param labelId The label ID assigned during training
     * @return Optional student
     */
    Optional<Student> findByLabelId(Integer labelId);

    /**
     * Checks if a student with the given name exists.
     * 
     * @param name The student's name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
