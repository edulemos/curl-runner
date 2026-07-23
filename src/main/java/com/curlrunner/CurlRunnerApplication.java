package com.curlrunner;

import javax.swing.SwingUtilities;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.curlrunner.ui.SwingMainFrame;

@SpringBootApplication
public class CurlRunnerApplication implements CommandLineRunner {

    private final SwingMainFrame mainFrame;

    public CurlRunnerApplication(SwingMainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(CurlRunnerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        SwingUtilities.invokeLater(() -> mainFrame.initialize());
    }
}
