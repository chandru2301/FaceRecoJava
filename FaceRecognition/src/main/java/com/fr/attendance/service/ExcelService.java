package com.fr.attendance.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for managing attendance records in Excel files.
 * Creates and updates attendance.xlsx with name, date, and status columns.
 */
@Slf4j
@Service
public class ExcelService {

    private static final String EXCEL_PATH = "attendance.xlsx";
    private static final String SHEET_NAME = "Attendance";
    private final ReentrantLock writeLock = new ReentrantLock();
    
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
     * Thread-safe with synchronization to prevent concurrent writes.
     * 
     * @param name The name of the person
     * @param department The department of the person
     * @param status The attendance status (Present/Absent)
     * @return true if attendance was successfully marked, false otherwise
     */
    public boolean markAttendance(String name, String department, String status) {
        // Synchronize to prevent concurrent writes
        writeLock.lock();
        try {
            File file = new File(EXCEL_PATH);
            File tempFile = new File(EXCEL_PATH + ".tmp");
            Workbook workbook = null;

            try {
                Sheet sheet;

                // Check if already marked today (before opening workbook)
                if (file.exists() && file.length() > 0) {
                    try (Workbook checkWorkbook = WorkbookFactory.create(file)) {
                        Sheet checkSheet = checkWorkbook.getSheet(SHEET_NAME);
                        if (checkSheet != null && isAlreadyMarked(checkSheet, name, LocalDate.now())) {
                            log.debug("{} already marked for today - skipping", name);
                            return false;
                        }
                    } catch (org.apache.poi.EmptyFileException e) {
                        log.warn("Excel file is corrupted or empty, will recreate: {}", e.getMessage());
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                }

                // Open or create workbook
                if (file.exists() && file.length() > 0) {
                    try {
                        workbook = WorkbookFactory.create(file);
                        sheet = workbook.getSheet(SHEET_NAME);
                        
                        if (sheet == null) {
                            sheet = workbook.createSheet(SHEET_NAME);
                            createHeaderRow(sheet);
                        }
                        
                        // Double-check if already marked (in case of race condition)
                        if (isAlreadyMarked(sheet, name, LocalDate.now())) {
                            log.debug("{} already marked for today (double-check) - skipping", name);
                            if (workbook != null) {
                                workbook.close();
                            }
                            return false;
                        }
                    } catch (org.apache.poi.EmptyFileException e) {
                        log.warn("Excel file is corrupted or empty, creating new workbook: {}", e.getMessage());
                        if (workbook != null) {
                            try {
                                workbook.close();
                            } catch (Exception ex) {
                                // Ignore
                            }
                        }
                        // Delete corrupted file
                        if (file.exists()) {
                            file.delete();
                        }
                        workbook = new XSSFWorkbook();
                        sheet = workbook.createSheet(SHEET_NAME);
                        createHeaderRow(sheet);
                    }
                } else {
                    // File doesn't exist or is empty, create new workbook
                    if (file.exists() && file.length() == 0) {
                        log.info("Empty Excel file detected, deleting and creating new file");
                        file.delete();
                    }
                    workbook = new XSSFWorkbook();
                    sheet = workbook.createSheet(SHEET_NAME);
                    createHeaderRow(sheet);
                }

                // Add new attendance record
                int newRowNum = sheet.getLastRowNum() + 1;
                Row row = sheet.createRow(newRowNum);
                row.createCell(0).setCellValue(name);
                row.createCell(1).setCellValue(department != null ? department : "");
                row.createCell(2).setCellValue(LocalDate.now().toString());
                row.createCell(3).setCellValue(status);

                // Write to temporary file first (atomic operation)
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    workbook.write(fos);
                    fos.flush();
                    fos.getFD().sync(); // Force write to disk
                }

                // Close workbook before renaming
                if (workbook != null) {
                    workbook.close();
                    workbook = null;
                }

                // Atomic rename: temp file -> actual file (with fallback for systems that don't support ATOMIC_MOVE)
                try {
                    Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (UnsupportedOperationException e) {
                    // Fallback for systems that don't support ATOMIC_MOVE (e.g., Windows)
                    log.debug("ATOMIC_MOVE not supported, using standard move");
                    Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                log.info("✅ {} marked as {}", name, status);
                return true;

            } catch (IOException e) {
                log.error("Error writing to Excel file: {}", e.getMessage(), e);
                // Clean up temp file if exists
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                // If file is corrupted, delete it
                if (file.exists() && (file.length() == 0 || e.getMessage() != null && e.getMessage().contains("ZLIB"))) {
                    log.warn("Deleting corrupted Excel file for next attempt");
                    file.delete();
                }
                return false;
            } catch (Exception e) {
                log.error("Unexpected error writing to Excel file: {}", e.getMessage(), e);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                return false;
            } finally {
                if (workbook != null) {
                    try {
                        workbook.close();
                    } catch (Exception e) {
                        log.error("Error closing workbook: {}", e.getMessage(), e);
                    }
                }
            }
        } finally {
            writeLock.unlock();
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
            Cell dateCell = row.getCell(2); // Date is in column 2 (0-indexed)

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

        if (!file.exists() || file.length() == 0) {
            log.debug("Excel file does not exist or is empty, returning empty set");
            return marked;
        }

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return marked;
            }

            String today = LocalDate.now().toString();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell nameCell = row.getCell(0);
                Cell dateCell = row.getCell(2); // Date is in column 2 (0-indexed)

                if (nameCell != null && dateCell != null) {
                    String rowDate = dateCell.getStringCellValue();
                    if (today.equals(rowDate)) {
                        marked.add(nameCell.getStringCellValue());
                    }
                }
            }
        } catch (org.apache.poi.EmptyFileException e) {
            log.warn("Excel file is empty (0 bytes), initializing as new file");
            // Delete the empty file so it can be recreated properly
            if (file.exists() && file.length() == 0) {
                file.delete();
            }
            return marked;
        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage(), e);
        }

        return marked;
    }
}
