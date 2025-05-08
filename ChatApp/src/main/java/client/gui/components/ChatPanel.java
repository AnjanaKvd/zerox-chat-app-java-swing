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
        
        // Messages panel with vertical BoxLayout
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        
        // Add to scroll pane
        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void addMessage(String message) {
        // Parse the message to determine type and extract data
        if (message.contains(" joined the chat at: ") || 
            message.contains(" left the chat at: ") ||
            message.contains("Chat started at: ") ||
            message.contains("Chat ended at: ")) {
            // System message
            addSystemMessage(message);
        } else {
            // Regular message - extract sender and content
            int colonPos = message.indexOf(": ");
            if (colonPos > 0) {
                String sender = message.substring(0, colonPos);
                String content = message.substring(colonPos + 2);
                
                // Extract timestamp if present
                Date timestamp = new Date();
                try {
                    // Check for timestamp at the end of message (format may vary)
                    int timestampStartPos = content.lastIndexOf(" [");
                    if (timestampStartPos > 0 && content.endsWith("]")) {
                        String timestampStr = content.substring(timestampStartPos + 2, content.length() - 1);
                        timestamp = TIMESTAMP_FORMAT.parse(timestampStr);
                        content = content.substring(0, timestampStartPos);
                    }
                } catch (ParseException e) {
                    // Use current time if parsing fails
                }
                
                // Check if we need to add a date header
                addDateHeaderIfNeeded(timestamp);
                
                // Create user object (simplified, you might want to look up the actual user)
                User senderUser = new User();
                senderUser.setNickname(sender);
                // Here you could look up the actual User object from a cache/database
                
                // Add the message
                addUserMessage(senderUser, content, sender.equals(currentUser.getNickname()), timestamp);
            } else {
                // Fallback for messages that don't match expected format
                addSystemMessage(message);
            }
        }
        
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) getComponent(0);
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }
    
    private void addSystemMessage(String message) {
        MessageBubble systemBubble = new MessageBubble(null, message, false, true, new Date());
        messagesPanel.add(systemBubble);
        messagesPanel.add(Box.createVerticalStrut(5));
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    
    private void addUserMessage(User sender, String content, boolean isCurrentUser, Date timestamp) {
        // If we only have the username but not the full user object
        if (sender != null && sender.getId() == 0 && !isCurrentUser) {
            // Try to fetch the user from the database
            String nickname = sender.getNickname();
            try {
                User fullUser = userDAO.findByUsernameOrNickname(nickname);
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
            // Add date header
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