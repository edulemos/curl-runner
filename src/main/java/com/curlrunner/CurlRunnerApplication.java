package com.curlrunner;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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

        // Aplica o tema Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Se Nimbus não estiver disponível, usa o padrão do sistema
        }

        SpringApplication.run(CurlRunnerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        SwingUtilities.invokeLater(() -> mainFrame.initialize());
    }
}
