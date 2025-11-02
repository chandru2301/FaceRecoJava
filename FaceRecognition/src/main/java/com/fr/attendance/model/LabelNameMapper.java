package com.fr.attendance.model;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map label IDs to person names.
 * Reads from label_names.txt file created during training.
 */
@Slf4j
public class LabelNameMapper {

    private static final String NAMES_PATH = "label_names.txt";
    private final Map<Integer, String> labelNameMap;

    public LabelNameMapper() {
        this.labelNameMap = loadNames();
    }

    /**
     * Loads the label-to-name mapping from the file.
     * 
     * @return Map of label ID to name
     */
    private Map<Integer, String> loadNames() {
        Map<Integer, String> map = new HashMap<>();
        File file = new File(NAMES_PATH);

        if (!file.exists()) {
            log.warn("Label names file not found: {}. Using empty map.", NAMES_PATH);
            return map;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    try {
                        int label = Integer.parseInt(parts[0]);
                        String name = parts[1];
                        map.put(label, name);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid line format in {}: {}", NAMES_PATH, line);
                    }
                }
            }
            log.info("Loaded {} label mappings from {}", map.size(), NAMES_PATH);
        } catch (IOException e) {
            log.error("Error reading label names file: {}", e.getMessage(), e);
        }

        return map;
    }

    /**
     * Gets the name for a given label ID.
     * 
     * @param label The label ID
     * @return The name, or "Unknown" if not found
     */
    public String getName(int label) {
        return labelNameMap.getOrDefault(label, "Unknown");
    }

    /**
     * Checks if a label ID exists in the mapping.
     * 
     * @param label The label ID
     * @return true if the label exists, false otherwise
     */
    public boolean containsLabel(int label) {
        return labelNameMap.containsKey(label);
    }

    /**
     * Gets the number of registered persons.
     * 
     * @return Number of registered persons
     */
    public int getPersonCount() {
        return labelNameMap.size();
    }
}
