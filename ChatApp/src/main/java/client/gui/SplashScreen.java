package client.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import java.awt.RenderingHints;

public class SplashScreen extends JWindow {
    private final JProgressBar progressBar;
    
    public SplashScreen() {
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 240, 255)); 
        
        JLabel titleLabel = new JLabel("ZeroX Chat Application", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(30, 50, 100)); 
        
        JLabel iconLabel = new JLabel(new ImageIcon(createImageIcon(64, 64)));
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        
        centerPanel.add(iconLabel, BorderLayout.CENTER);
        centerPanel.add(titleLabel, BorderLayout.SOUTH);
        content.add(centerPanel, BorderLayout.CENTER);
        
        
        JLabel versionLabel = new JLabel("Version 1.0.0", JLabel.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        content.add(versionLabel, BorderLayout.NORTH);
        
        JLabel copyrightLabel = new JLabel("© 2023 Zero X Team", JLabel.CENTER);
        copyrightLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(copyrightLabel, BorderLayout.SOUTH);
        content.add(bottomPanel, BorderLayout.SOUTH);
        
        
        setContentPane(content);
        
        
        setSize(400, 300);
        setLocationRelativeTo(null);
        
        
        startLoadingAnimation();
    }
    
    private void startLoadingAnimation() {
        Timer timer = new Timer(2000, e -> {
            
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    
    private Image createImageIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        
        g2d.setColor(new Color(65, 105, 225)); 
        g2d.fillRoundRect(5, 5, width-10, height-10, 15, 15);
        
        
        g2d.setColor(Color.WHITE);
        g2d.fillOval(width/4, height/3, width/5, width/5);
        g2d.fillOval(width/2, height/3, width/5, width/5);
        g2d.fillOval(3*width/4, height/3, width/5, width/5);
        
        g2d.dispose();
        return image;
    }
} 