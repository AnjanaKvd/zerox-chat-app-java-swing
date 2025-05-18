package client.gui.components;

import model.User;
import dao.UserDAO;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatPanel extends JPanel {
    private final User currentUser;
    private final JPanel messagesPanel;
    private final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat DATE_HEADER_FORMAT = new SimpleDateFormat("MMMM d, yyyy");
    private Date lastDateHeader = null;
    private final UserDAO userDAO;
    
    public ChatPanel(User currentUser) {
        this.currentUser = currentUser;
        this.userDAO = new UserDAO();
        
        setLayout(new BorderLayout());
        
        // Panel for messages with vertical BoxLayout
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        
        // Scroll pane for the messages panel
        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void addMessage(String message) {
        // Check for message type identifiers
        String messageType = "TEXT";
        String content = message;
        
        // Extract message type from identifier if present
        if (message.startsWith("[") && message.contains("]")) {
            int closeBracketPos = message.indexOf("]");
            if (closeBracketPos > 1) {
                messageType = message.substring(1, closeBracketPos);
                content = message.substring(closeBracketPos + 1);
            }
        }
        
        // Process message based on its type
        if (messageType.equals("HEADER")) {
            // Header message handling (optional - typically only in logs)
            JLabel headerLabel = new JLabel(content);
            headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            headerLabel.setForeground(new Color(0, 102, 204));
            headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
            messagesPanel.add(headerLabel);
        }
        else if (messageType.equals("ADMIN")) {
            // Admin message handling
            JLabel adminLabel = new JLabel(content);
            adminLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            adminLabel.setForeground(Color.GRAY);
            adminLabel.setHorizontalAlignment(SwingConstants.CENTER);
            adminLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            adminLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            messagesPanel.add(adminLabel);
        }
        else if (messageType.equals("SEPARATOR")) {
            // Separator line
            JSeparator separator = new JSeparator();
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            separator.setForeground(Color.LIGHT_GRAY);
            messagesPanel.add(separator);
            messagesPanel.add(Box.createVerticalStrut(10));
        }
        else if (messageType.equals("JOIN") || messageType.equals("LEAVE")) {
            // User join/leave system message
            // Process timestamp for date headers
            try {
                int colonIndex = content.lastIndexOf(": ");
                if (colonIndex > 0) {
                    String timeStr = content.substring(colonIndex + 2);
                    Date messageDate = TIMESTAMP_FORMAT.parse(timeStr);
                    addDateHeaderIfNeeded(messageDate);
                }
            } catch (ParseException e) {
                // Continue without date header if we can't parse
            }
            
            // Format the join/leave message
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
            addSystemMessage(systemMessage);
        }
        else if (messageType.equals("MSG")) {
            // Regular user message
            int colonPos = content.indexOf(": ");
            if (colonPos > 0) {
                String sender = content.substring(0, colonPos);
                String messageContent = content.substring(colonPos + 2);
                
                // Get timestamp if available
                Date timestamp = new Date();
                
                // Add user message with appropriate styling
                addUserMessage(sender, messageContent, timestamp);
            } else {
                // Fallback if message format is unexpected
                addSystemMessage(content);
            }
        }
        else if (messageType.equals("SYSTEM")) {
            // System message
            addSystemMessage(content);
        }
        else {
            // Legacy format without identifiers
            if (content.contains(" has joined : ") || 
                content.contains(" left : ") ||
                content.contains("Chat started at: ") ||
                content.contains("Chat ended at: ")) {
                
                addSystemMessage(content);
            } else {
                // Regular message with format: sender: message
                int colonPos = content.indexOf(": ");
                if (colonPos > 0) {
                    String sender = content.substring(0, colonPos);
                    String messageContent = content.substring(colonPos + 2);
                    
                    // Get timestamp if available
                    Date timestamp = new Date();
                    try {
                        int timestampStartPos = messageContent.lastIndexOf(" [");
                        if (timestampStartPos > 0 && messageContent.endsWith("]")) {
                            String timestampStr = messageContent.substring(timestampStartPos + 2, messageContent.length() - 1);
                            timestamp = TIMESTAMP_FORMAT.parse(timestampStr);
                            messageContent = messageContent.substring(0, timestampStartPos);
                        }
                    } catch (ParseException e) {
                        // Use current time if parsing fails
                    }
                    
                    // Add date header if needed
                    addDateHeaderIfNeeded(timestamp);
                    
                    // Add the user message
                    addUserMessage(sender, messageContent, timestamp);
                } else {
                    // Fallback for unrecognized format
                    addSystemMessage(content);
                }
            }
        }
        
        // Auto-scroll to the bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) getComponent(0);
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }
    
    private void addSystemMessage(String message) {
        // Format system message for better display
        String formattedMessage = message;
        
        // Strip timestamp info for cleaner display if present
        if (message.contains(" joined the chat at: ")) {
            int idx = message.indexOf(" joined the chat at: ");
            String username = message.substring(0, idx);
            formattedMessage = username + " joined the chat";
        } else if (message.contains(" left the chat at: ")) {
            int idx = message.indexOf(" left the chat at: ");
            String username = message.substring(0, idx);
            formattedMessage = username + " left the chat";
        } else if (message.contains("Chat started at: ")) {
            formattedMessage = "Chat started";
        } else if (message.contains("Chat ended at: ")) {
            formattedMessage = "Chat ended";
        }
        
        MessageBubble systemBubble = new MessageBubble(null, formattedMessage, false, true, new Date());
        messagesPanel.add(systemBubble);
        messagesPanel.add(Box.createVerticalStrut(5));
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    
    private void addUserMessage(String senderName, String content, Date timestamp) {
        // Try to get full user info
        User sender = new User();
        sender.setNickname(senderName);
        
        boolean isCurrentUser = senderName.equals(currentUser.getNickname());
        
        if (!isCurrentUser) {
            try {
                User fullUser = userDAO.findByUsernameOrNickname(senderName);
                if (fullUser != null) {
                    sender = fullUser;
                }
            } catch (Exception e) {
                System.err.println("Error fetching user data: " + e.getMessage());
            }
        }
        
        MessageBubble messageBubble = new MessageBubble(sender, content, isCurrentUser, false, timestamp);
        messagesPanel.add(messageBubble);
        messagesPanel.add(Box.createVerticalStrut(2));
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    
    private void addDateHeaderIfNeeded(Date messageDate) {
        Calendar messageCal = Calendar.getInstance();
        messageCal.setTime(messageDate);
        messageCal.set(Calendar.HOUR_OF_DAY, 0);
        messageCal.set(Calendar.MINUTE, 0);
        messageCal.set(Calendar.SECOND, 0);
        messageCal.set(Calendar.MILLISECOND, 0);
        Date messageDay = messageCal.getTime();
        
        if (lastDateHeader == null || !messageDay.equals(lastDateHeader)) {
            // Add a date header
            JLabel dateLabel = new JLabel(DATE_HEADER_FORMAT.format(messageDate));
            dateLabel.setForeground(Color.GRAY);
            dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            dateLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
            
            messagesPanel.add(dateLabel);
            lastDateHeader = messageDay;
        }
    }
    
    public void clear() {
        messagesPanel.removeAll();
        lastDateHeader = null;
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
} 