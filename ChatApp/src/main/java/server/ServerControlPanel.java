package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerControlPanel extends JFrame {
    private JTextArea logArea;
    private JButton shutdownButton;
    private JLabel statusLabel;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    public ServerControlPanel() {
        setTitle("Chat Server Control Panel");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmShutdown();
            }
        });
        
        logMessage("Server control panel started");
    }
    
    private void initComponents() {
        
        statusLabel = new JLabel("Server Status: RUNNING");
        statusLabel.setForeground(Color.GREEN);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        shutdownButton = new JButton("Shutdown Server");
        shutdownButton.setBackground(Color.RED);
        shutdownButton.setForeground(Color.WHITE);
        shutdownButton.addActionListener(e -> confirmShutdown());
        buttonPanel.add(shutdownButton);
        
        
        setLayout(new BorderLayout());
        add(statusLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void confirmShutdown() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to shutdown the server?\nAll connected clients will be disconnected.",
                "Confirm Shutdown",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
        if (result == JOptionPane.YES_OPTION) {
            shutdownServer();
        }
    }
    
    private void shutdownServer() {
        logMessage("Server shutting down...");
        statusLabel.setText("Server Status: SHUTTING DOWN");
        statusLabel.setForeground(Color.RED);
        
        
        Timer timer = new Timer(1000, e -> {
            System.out.println("Server shutdown completed");
            System.exit(0);
        });
        timer.setRepeats(false);
        timer.start();
    }
} 