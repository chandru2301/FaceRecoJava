package com.fr.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for student registration request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationDto {

   
    private String name;

    private String department;

    private MultipartFile image;
}
