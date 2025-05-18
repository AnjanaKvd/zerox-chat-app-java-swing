package client.gui;

import client.gui.components.MessageBubble;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatLogViewer extends JFrame {
    private JPanel messagesPanel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Date lastDateHeader = null;
    private final SimpleDateFormat dateHeaderFormat = new SimpleDateFormat("MMMM d, yyyy");
    
    public ChatLogViewer(String logFileName) {
        setTitle("Chat Log: " + logFileName);
        setSize(700, 500);
        setLocationRelativeTo(null);
        
        // Use a panel with BoxLayout instead of JTextArea for styled messages
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Load and parse the log file
        loadLogFile(logFileName);
        
        setVisible(true);
    }
    
    private void loadLogFile(String logFileName) {
        File logFile = new File(logFileName);
        
        if (!logFile.exists()) {
            JLabel errorLabel = new JLabel("Error: Log file not found: " + logFile.getAbsolutePath());
            errorLabel.setForeground(Color.RED);
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messagesPanel.add(errorLabel);
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                parseLine(line);
            }
            
            // Scroll to the top
            SwingUtilities.invokeLater(() -> {
                JScrollPane scrollPane = (JScrollPane) getContentPane().getComponent(0);
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                verticalBar.setValue(0);
            });
            
        } catch (IOException e) {
            JLabel errorLabel = new JLabel("Error reading log file: " + e.getMessage());
            errorLabel.setForeground(Color.RED);
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messagesPanel.add(errorLabel);
        }
    }
    
    private void parseLine(String line) {
        // Check for message type identifiers
        String messageType = "TEXT";
        String content = line;
        
        // Extract message type from identifier if present
        if (line.startsWith("[") && line.contains("]")) {
            int closeBracketPos = line.indexOf("]");
            if (closeBracketPos > 1) {
                messageType = line.substring(1, closeBracketPos);
                content = line.substring(closeBracketPos + 1);
            }
        }
        // If no identifier, try to detect message type
        else {
            // Chat header identifier
            if (line.startsWith("Chat '") && line.contains("' created at ")) {
                messageType = "HEADER";
            } 
            // Creator info
            else if (line.startsWith("Created by admin: ")) {
                messageType = "ADMIN_INFO";
            }
            // User join
            else if (line.contains(" has joined : ")) {
                messageType = "JOIN";
            } 
            // User leave
            else if (line.contains(" left : ")) {
                messageType = "LEAVE";
            } 
            // Message with nickname
            else if (line.contains(": ")) {
                messageType = "MSG";
            }
        }
        
        // Extract timestamp for date headers if present
        try {
            if (messageType.equals("JOIN") || messageType.equals("LEAVE")) {
                int colonIndex = content.lastIndexOf(": ");
                if (colonIndex > 0) {
                    String timeStr = content.substring(colonIndex + 2);
                    Date messageDate = dateFormat.parse(timeStr);
                    addDateHeaderIfNeeded(messageDate);
                }
            }
        } catch (ParseException e) {
            // If we can't parse date, continue without date header
        }
        
        // Handle message based on type
        switch (messageType) {
            case "HEADER":
                JLabel headerLabel = new JLabel(content);
                headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
                headerLabel.setForeground(new Color(0, 102, 204));
                headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
                headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
                
                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                headerPanel.setBackground(new Color(240, 240, 240, 150));
                headerPanel.add(headerLabel);
                headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height));
                
                messagesPanel.add(headerPanel);
                break;
                
            case "ADMIN":
            case "ADMIN_INFO":
                JLabel adminLabel = new JLabel(content);
                adminLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                adminLabel.setForeground(Color.GRAY);
                adminLabel.setHorizontalAlignment(SwingConstants.CENTER);
                adminLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
                messagesPanel.add(adminLabel);
                break;
                
            case "SEPARATOR":
                JSeparator separator = new JSeparator();
                separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                separator.setForeground(Color.LIGHT_GRAY);
                messagesPanel.add(separator);
                messagesPanel.add(Box.createVerticalStrut(10));
                break;
                
            case "JOIN":
            case "LEAVE":
                // Format: username has joined/left : timestamp
                String username;
                String action;
                
                if (messageType.equals("JOIN")) {
                    int idx = content.indexOf(" has joined : ");
                    username = content.substring(0, idx);
                    action = "joined the chat";
                } else {
                    int idx = content.indexOf(" left : ");
                    username = content.substring(0, idx);
                    action = "left the chat";
                }
                
                String systemMessage = username + " " + action;
                MessageBubble systemBubble = new MessageBubble(null, systemMessage, false, true, new Date());
                messagesPanel.add(systemBubble);
                messagesPanel.add(Box.createVerticalStrut(5));
                break;
                
            case "MSG":
                int colonPos = content.indexOf(": ");
                if (colonPos > 0) {
                    String sender = content.substring(0, colonPos);
                    String messageContent = content.substring(colonPos + 2);
                    
                    User senderUser = new User();
                    senderUser.setNickname(sender);
                    
                    // Compare with current user's nickname to determine message alignment
                    boolean isCurrentUser = sender.equals("anj"); // Ideally get this from actual current user
                    
                    // Add user message with the appropriate style
                    MessageBubble messageBubble = new MessageBubble(senderUser, messageContent, isCurrentUser);
                    messagesPanel.add(messageBubble);
                    messagesPanel.add(Box.createVerticalStrut(2));
                }
                break;
                
            case "SYSTEM":
                JLabel systemLabel = new JLabel(content);
                systemLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                systemLabel.setForeground(new Color(100, 100, 100));
                systemLabel.setHorizontalAlignment(SwingConstants.CENTER);
                
                JPanel systemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                systemPanel.setBackground(new Color(245, 245, 245));
                systemPanel.add(systemLabel);
                systemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, systemPanel.getPreferredSize().height));
                
                messagesPanel.add(systemPanel);
                messagesPanel.add(Box.createVerticalStrut(5));
                break;
                
            default:
                // Default text
                JLabel textLabel = new JLabel(content);
                textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                messagesPanel.add(textLabel);
                break;
        }
        
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    
    private void addDateHeaderIfNeeded(Date messageDate) {
        if (messageDate == null) return;
        
        // Set time to 00:00:00 to compare just the date part
        java.util.Calendar messageCal = java.util.Calendar.getInstance();
        messageCal.setTime(messageDate);
        messageCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        messageCal.set(java.util.Calendar.MINUTE, 0);
        messageCal.set(java.util.Calendar.SECOND, 0);
        messageCal.set(java.util.Calendar.MILLISECOND, 0);
        Date messageDay = messageCal.getTime();
        
        if (lastDateHeader == null || !messageDay.equals(lastDateHeader)) {
            // Add date header
            JLabel dateLabel = new JLabel(dateHeaderFormat.format(messageDate));
            dateLabel.setForeground(Color.GRAY);
            dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            dateLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
            
            messagesPanel.add(dateLabel);
            lastDateHeader = messageDay;
        }
    }
}