package client.gui.components;

import model.Chat;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCard extends JPanel {
    private final Chat chat;
    private final User currentUser;
    private final Color BADGE_COLOR = new Color(232, 76, 61);
    private final Color HOVER_COLOR = new Color(242, 242, 242);
    private final Color NORMAL_BACKGROUND = Color.WHITE;
    private final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private final Font CONTENT_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    
    private JLabel chatNameLabel;
    private JLabel avatarLabel;
    private JLabel messageLabel;
    private JLabel timeLabel;
    private JLabel unreadBadge;
    
    private String placeholderMessage = "No messages yet";
    private int unreadCount = 0; 
    
    public ChatCard(Chat chat, User currentUser) {
        this.chat = chat;
        this.currentUser = currentUser;
        
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        setBackground(NORMAL_BACKGROUND);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); // Set fixed height for consistent appearance
        
        initComponents();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(NORMAL_BACKGROUND);
            }
        });
    }
    
    private void initComponents() {
        // Avatar setup
        User admin = chat.getAdmin();
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(50, 50));
        setAvatar(admin);
        
        // Content panel setup
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Determine chat name with fallbacks
        String chatName;
        if (chat.getName() != null && !chat.getName().isEmpty()) {
            chatName = chat.getName();
        } else if (admin != null && admin.getNickname() != null && !admin.getNickname().isEmpty()) {
            chatName = admin.getNickname() + "'s Chat";
        } else if (admin != null && admin.getUsername() != null && !admin.getUsername().isEmpty()) {
            chatName = admin.getUsername() + "'s Chat";
        } else {
            chatName = "Chat #" + chat.getId();
        }
        
        // Chat name label
        chatNameLabel = new JLabel(chatName);
        chatNameLabel.setFont(TITLE_FONT);
        chatNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Time label
        timeLabel = new JLabel(TIME_FORMAT.format(chat.getStartTime() != null ? chat.getStartTime() : new Date()));
        timeLabel.setFont(CONTENT_FONT);
        timeLabel.setForeground(Color.GRAY);
        
        // Message preview label
        messageLabel = new JLabel(placeholderMessage);
        messageLabel.setFont(CONTENT_FONT);
        messageLabel.setForeground(Color.GRAY);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Top panel with chat name and time
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(chatNameLabel, BorderLayout.WEST);
        topPanel.add(timeLabel, BorderLayout.EAST);
        
        // Add components to content panel
        contentPanel.add(topPanel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(messageLabel);
        
        // Setup layout
        add(avatarLabel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        
        // Load chat info
        loadLatestMessageAndUnreadCount();
    }
    
    private void setAvatar(User user) {
        ImageIcon avatar = null;
        int size = 50; // Increased size
        
        if (user != null && user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
            File imgFile = new File(user.getProfilePic());
            if (imgFile.exists() && imgFile.isFile()) {
                try {
                    avatar = new ImageIcon(user.getProfilePic());
                    if (avatar.getImageLoadStatus() == java.awt.MediaTracker.ERRORED) {
                        avatar = null;
                    }
                } catch (Exception e) {
                    avatar = null;
                }
            }
        }
        
        if (avatar == null) {
            // Generate avatar with gradient background
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Create gradient background
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(52, 152, 219), 
                size, size, new Color(41, 128, 185)
            );
            g2d.setPaint(gradient);
            g2d.fillOval(0, 0, size, size);
            
            // Draw initial
            g2d.setColor(Color.WHITE);
            String initial = user != null && user.getNickname() != null && !user.getNickname().isEmpty() ? 
                             user.getNickname().substring(0, 1).toUpperCase() : 
                             (chat.getName() != null && !chat.getName().isEmpty() ? 
                              chat.getName().substring(0, 1).toUpperCase() : "C");
            Font font = new Font("Segoe UI", Font.BOLD, size / 2);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int x = (size - fm.stringWidth(initial)) / 2;
            int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(initial, x, y);
            g2d.dispose();
            
            avatar = new ImageIcon(img);
        } else {
            // Make circular avatar from existing image
            Image sourceImage = avatar.getImage();
            Image scaledImage = sourceImage.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            
            BufferedImage circularImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = circularImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(new Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(scaledImage, 0, 0, size, size, null);
            
            // Add a border
            g2.setColor(new Color(200, 200, 200));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(0, 0, size-1, size-1);
            g2.dispose();
            
            avatar = new ImageIcon(circularImage);
        }
        
        avatarLabel.setIcon(avatar);
    }
    
    private void loadLatestMessageAndUnreadCount() {
        String logFile = chat.getLogFile();
        if (logFile != null && !logFile.isEmpty()) {
            File file = new File(logFile);
            if (file.exists()) {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                    if (!lines.isEmpty()) {
                        // Find the latest actual message (not system messages)
                        String latestMsg = null;
                        for (int i = lines.size() - 1; i >= 0; i--) {
                            String line = lines.get(i);
                            
                            // Check for proper message format with the identifier
                            if (line.startsWith("[MSG]")) {
                                latestMsg = line.substring(5); // Remove [MSG] prefix
                                break;
                            } else if (line.contains(": ") && 
                                     !line.contains(" joined the chat at: ") && 
                                     !line.contains(" left the chat at: ") &&
                                     !line.contains("Chat started at: ") &&
                                     !line.contains("Chat ended at: ")) {
                                latestMsg = line;
                                break;
                            }
                        }
                        
                        if (latestMsg != null) {
                            // Extract sender and message
                            int colonPos = latestMsg.indexOf(": ");
                            if (colonPos > 0) {
                                String sender = latestMsg.substring(0, colonPos);
                                String messageContent = latestMsg.substring(colonPos + 2);
                                
                                // Truncate message if needed
                                if (messageContent.length() > 35) {
                                    messageContent = messageContent.substring(0, 32) + "...";
                                }
                                
                                // Format the preview
                                messageLabel.setText("<html><b>" + sender + ":</b> " + messageContent + "</html>");
                            } else {
                                messageLabel.setText(latestMsg);
                            }
                            
                            // Try to extract time from message for the time label
                            Pattern timePattern = Pattern.compile("\\d{2}:\\d{2}(:\\d{2})?");
                            Matcher matcher = timePattern.matcher(latestMsg);
                            if (matcher.find()) {
                                timeLabel.setText(matcher.group());
                            } else {
                                timeLabel.setText(TIME_FORMAT.format(new Date()));
                            }
                        }
                        
                        // Count unread messages
                        countUnreadMessages(lines);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading chat info: " + e.getMessage());
                }
            }
        }
    }
    
    private void countUnreadMessages(java.util.List<String> lines) {
        int unread = 0;
        String lastLeavePattern = currentUser.getNickname() + " left the chat at: ";
        String lastLeaveTime = null;
        
        // Find the last time the user left the chat
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).contains(lastLeavePattern)) {
                lastLeaveTime = lines.get(i).substring(lastLeavePattern.length());
                break;
            }
        }
        
        if (lastLeaveTime != null) {
            // Count messages after the user left
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                
                if (line.startsWith("[MSG]") && !line.contains(currentUser.getNickname() + ": ")) {
                    try {
                        // Extract timestamp if available and compare
                        int timeIndex = line.lastIndexOf(":");
                        if (timeIndex > 0 && timeIndex < line.length() - 1) {
                            String msgTime = line.substring(timeIndex - 8, timeIndex + 3);
                            if (msgTime.compareTo(lastLeaveTime) > 0) {
                                unread++;
                            }
                        }
                    } catch (Exception e) {
                        // If we can't extract the time, count it as unread
                        unread++;
                    }
                }
            }
        } else {
            // If user never left, count all messages not from the user
            for (String line : lines) {
                if (line.startsWith("[MSG]") && !line.contains(currentUser.getNickname() + ": ")) {
                    unread++;
                }
            }
        }
        
        // Update badge if there are unread messages
        if (unread > 0) {
            updateUnreadBadge(unread);
        }
    }
    
    private void updateUnreadBadge(int count) {
        this.unreadCount = count;
        
        // Remove existing badge if any
        if (unreadBadge != null) {
            remove(unreadBadge);
        }
        
        // Create new badge
        unreadBadge = new JLabel(String.valueOf(count));
        unreadBadge.setOpaque(true);
        unreadBadge.setBackground(BADGE_COLOR);
        unreadBadge.setForeground(Color.WHITE);
        unreadBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        unreadBadge.setHorizontalAlignment(SwingConstants.CENTER);
        unreadBadge.setVerticalAlignment(SwingConstants.CENTER);
        
        // Make the badge circular
        int size = 22;
        unreadBadge.setPreferredSize(new Dimension(size, size));
        unreadBadge.setBorder(BorderFactory.createEmptyBorder());
        
        // Add the badge
        add(unreadBadge, BorderLayout.EAST);
        
        revalidate();
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Make badge circular if it exists
        if (unreadBadge != null) {
            Graphics2D g2d = (Graphics2D) unreadBadge.getGraphics();
            if (g2d != null) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.fillOval(0, 0, unreadBadge.getWidth(), unreadBadge.getHeight());
            }
        }
    }
    
    public Chat getChat() {
        return chat;
    }
} 