package com.fr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FaceRecognitionApplication {

	public static void main(String[] args) {
		// On Windows, try to enable GUI mode if not explicitly set
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			String headless = System.getProperty("java.awt.headless");
			if (headless == null || "true".equals(headless)) {
				System.setProperty("java.awt.headless", "false");
				System.out.println("GUI mode enabled for Windows desktop environment");
			}
		}
		
		SpringApplication.run(FaceRecognitionApplication.class, args);
	}

}
