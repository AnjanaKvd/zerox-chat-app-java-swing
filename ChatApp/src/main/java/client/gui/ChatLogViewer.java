package client.gui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ChatLogViewer extends JFrame {
    private JTextArea logArea;
    
    public ChatLogViewer(String logFileName) {
        setTitle("Chat Log: " + logFileName);
        setSize(700, 500);
        setLocationRelativeTo(null);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add a close button at the bottom
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Load the log file content
        loadLogFile(logFileName);
        
        setVisible(true);
    }
    
    private void loadLogFile(String logFileName) {
        File logFile = new File("logs", logFileName);
        
        if (!logFile.exists()) {
            logArea.setText("Error: Log file not found: " + logFile.getAbsolutePath());
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            logArea.setText(content.toString());
            logArea.setCaretPosition(0); // Scroll to top
            
        } catch (IOException e) {
            logArea.setText("Error reading log file: " + e.getMessage());
        }
    }
}