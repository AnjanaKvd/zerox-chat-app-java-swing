package client.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
//import java.awt.image.Graphics2D;
import java.awt.RenderingHints;

public class SplashScreen extends JWindow {
    private final JProgressBar progressBar;
    
    public SplashScreen() {
        // Create content panel
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        // Use a simple label instead of trying to load an image
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 240, 255)); // Light blue background
        
        JLabel titleLabel = new JLabel("ZeroX Chat Application", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(30, 50, 100)); // Dark blue text
        
        JLabel iconLabel = new JLabel(new ImageIcon(createImageIcon(64, 64)));
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        
        centerPanel.add(iconLabel, BorderLayout.CENTER);
        centerPanel.add(titleLabel, BorderLayout.SOUTH);
        content.add(centerPanel, BorderLayout.CENTER);
        
        // Add version and copyright info
        JLabel versionLabel = new JLabel("Version 1.0.0", JLabel.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        content.add(versionLabel, BorderLayout.NORTH);
        
        JLabel copyrightLabel = new JLabel("© 2023 Zero X Team", JLabel.CENTER);
        copyrightLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        
        // Add progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(copyrightLabel, BorderLayout.SOUTH);
        content.add(bottomPanel, BorderLayout.SOUTH);
        
        // Add all content to window
        setContentPane(content);
        
        // Set window size and center on screen
        setSize(400, 300);
        setLocationRelativeTo(null);
        
        // Start loading animation
        startLoadingAnimation();
    }
    
    private void startLoadingAnimation() {
        Timer timer = new Timer(2000, e -> {
            // This will be handled by the worker in the main class
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    // Create a simple programmatic chat bubble icon rather than loading from a file
    private Image createImageIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw chat bubble
        g2d.setColor(new Color(65, 105, 225)); // Royal blue
        g2d.fillRoundRect(5, 5, width-10, height-10, 15, 15);
        
        // Draw chat symbol
        g2d.setColor(Color.WHITE);
        g2d.fillOval(width/4, height/3, width/5, width/5);
        g2d.fillOval(width/2, height/3, width/5, width/5);
        g2d.fillOval(3*width/4, height/3, width/5, width/5);
        
        g2d.dispose();
        return image;
    }
} 