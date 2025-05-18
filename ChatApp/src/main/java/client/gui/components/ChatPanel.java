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
        
        
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);
        
        
        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void addMessage(String message) {
        
        if (message.contains(" joined the chat at: ") || 
            message.contains(" left the chat at: ") ||
            message.contains("Chat started at: ") ||
            message.contains("Chat ended at: ")) {
            
            addSystemMessage(message);
        } else {
            
            int colonPos = message.indexOf(": ");
            if (colonPos > 0) {
                String sender = message.substring(0, colonPos);
                String content = message.substring(colonPos + 2);
                
                
                Date timestamp = new Date();
                try {
                    
                    int timestampStartPos = content.lastIndexOf(" [");
                    if (timestampStartPos > 0 && content.endsWith("]")) {
                        String timestampStr = content.substring(timestampStartPos + 2, content.length() - 1);
                        timestamp = TIMESTAMP_FORMAT.parse(timestampStr);
                        content = content.substring(0, timestampStartPos);
                    }
                } catch (ParseException e) {
                    
                }
                
                
                addDateHeaderIfNeeded(timestamp);
                
                
                User senderUser = new User();
                senderUser.setNickname(sender);
                
                try {
                    User fullUser = userDAO.findByUsernameOrNickname(sender);
                    if (fullUser != null) {
                        senderUser = fullUser;
                    }
                } catch (Exception e) {
                    
                }
                
                
                addUserMessage(senderUser, content, sender.equals(currentUser.getNickname()), timestamp);
            } else {
                
                addSystemMessage(message);
            }
        }
        
        
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) getComponent(0);
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }
    
    private void addSystemMessage(String message) {
        
        String formattedMessage = message;
        
        
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
    
    private void addUserMessage(User sender, String content, boolean isCurrentUser, Date timestamp) {
        
        if (sender != null && sender.getId() == 0 && !isCurrentUser) {
            
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