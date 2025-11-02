package com.fr.attendance.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing attendance records in Excel files.
 * Creates and updates attendance.xlsx with name, date, and status columns.
 */
@Slf4j
@Service
public class ExcelService {

    private static final String EXCEL_PATH = "attendance.xlsx";
    private static final String SHEET_NAME = "Attendance";
    
    /**
     * Gets the absolute path to the attendance Excel file.
     * 
     * @return Absolute path to attendance.xlsx
     */
    public String getExcelFilePath() {
        File file = new File(EXCEL_PATH);
        return file.getAbsolutePath();
    }

    /**
     * Marks attendance for a person in the Excel file.
     * 
     * @param name The name of the person
     * @param department The department of the person
     * @param status The attendance status (Present/Absent)
     * @return true if attendance was successfully marked, false otherwise
     */
    public boolean markAttendance(String name, String department, String status) {
        File file = new File(EXCEL_PATH);
        Workbook workbook = null;

        try {
            Sheet sheet;

            if (file.exists()) {
                workbook = WorkbookFactory.create(file);
                sheet = workbook.getSheet(SHEET_NAME);
                
                if (sheet == null) {
                    sheet = workbook.createSheet(SHEET_NAME);
                    createHeaderRow(sheet);
                }
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(SHEET_NAME);
                createHeaderRow(sheet);
            }

            // Check if already marked today
            if (isAlreadyMarked(sheet, name, LocalDate.now())) {
                log.debug("{} already marked for today", name);
                return false;
            }

            // Add new attendance record
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            row.createCell(0).setCellValue(name);
            row.createCell(1).setCellValue(department != null ? department : "");
            row.createCell(2).setCellValue(LocalDate.now().toString());
            row.createCell(3).setCellValue(status);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            log.info("✅ {} marked as {}", name, status);
            return true;

        } catch (IOException e) {
            log.error("Error writing to Excel file: {}", e.getMessage(), e);
            return false;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("Error closing workbook: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Checks if a person has already been marked for today.
     * 
     * @param sheet The Excel sheet
     * @param name The person's name
     * @param date The date to check
     * @return true if already marked, false otherwise
     */
    private boolean isAlreadyMarked(Sheet sheet, String name, LocalDate date) {
        String dateStr = date.toString();
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell nameCell = row.getCell(0);
            Cell dateCell = row.getCell(1);

            if (nameCell != null && dateCell != null) {
                String rowName = nameCell.getStringCellValue();
                String rowDate = dateCell.getStringCellValue();

                if (name.equals(rowName) && dateStr.equals(rowDate)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates the header row in the Excel sheet.
     * 
     * @param sheet The Excel sheet
     */
    private void createHeaderRow(Sheet sheet) {
        Row header = sheet.createRow(0);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        Cell nameCell = header.createCell(0);
        nameCell.setCellValue("Name");
        nameCell.setCellStyle(headerStyle);

        Cell departmentCell = header.createCell(1);
        departmentCell.setCellValue("Department");
        departmentCell.setCellStyle(headerStyle);

        Cell dateCell = header.createCell(2);
        dateCell.setCellValue("Date");
        dateCell.setCellStyle(headerStyle);

        Cell statusCell = header.createCell(3);
        statusCell.setCellValue("Status");
        statusCell.setCellStyle(headerStyle);
    }

    /**
     * Gets all names that have been marked today.
     * 
     * @return Set of names marked today
     */
    public Set<String> getMarkedToday() {
        Set<String> marked = new HashSet<>();
        File file = new File(EXCEL_PATH);

        if (!file.exists()) {
            return marked;
        }

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                return marked;
            }

            String today = LocalDate.now().toString();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell nameCell = row.getCell(0);
                Cell dateCell = row.getCell(1);

                if (nameCell != null && dateCell != null) {
                    String rowDate = dateCell.getStringCellValue();
                    if (today.equals(rowDate)) {
                        marked.add(nameCell.getStringCellValue());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage(), e);
        }

        return marked;
    }
}
