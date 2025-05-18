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
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        setBackground(NORMAL_BACKGROUND);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
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
        
        User admin = chat.getAdmin();
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(40, 40));
        setAvatar(admin);
        
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        
        
        String chatName;
        if (chat.getName() != null && !chat.getName().isEmpty()) {
            chatName = chat.getName();
        } else if (admin != null) {
            chatName = admin.getUsername() + "'s Chat";
        } else {
            chatName = "Chat #" + chat.getId();
        }
        
        chatNameLabel = new JLabel(chatName);
        chatNameLabel.setFont(TITLE_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(chatNameLabel, gbc);
        
        
        timeLabel = new JLabel(TIME_FORMAT.format(chat.getStartTime() != null ? chat.getStartTime() : new Date()));
        timeLabel.setFont(CONTENT_FONT);
        timeLabel.setForeground(Color.GRAY);
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(timeLabel, gbc);
        
        
        messageLabel = new JLabel(placeholderMessage);
        messageLabel.setFont(CONTENT_FONT);
        messageLabel.setForeground(Color.GRAY);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(messageLabel, gbc);
        
        
        if (Math.random() > 0.5) { 
            unreadCount = (int)(Math.random() * 10) + 1;
            unreadBadge = new JLabel(String.valueOf(unreadCount));
            unreadBadge.setOpaque(true);
            unreadBadge.setBackground(BADGE_COLOR);
            unreadBadge.setForeground(Color.WHITE);
            unreadBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            unreadBadge.setHorizontalAlignment(SwingConstants.CENTER);
            unreadBadge.setPreferredSize(new Dimension(20, 20));
            
            
            unreadBadge.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.EAST;
            contentPanel.add(unreadBadge, gbc);
        }
        
        add(avatarLabel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        
        
        loadLatestMessageAndUnreadCount();
    }
    
    private void setAvatar(User user) {
        ImageIcon avatar = null;
        int size = 40;
        
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
            
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            
            g2d.setColor(new Color(52, 152, 219));
            g2d.fillOval(0, 0, size, size);
            
            
            g2d.setColor(Color.WHITE);
            String initial = user != null && user.getNickname() != null && !user.getNickname().isEmpty() ? 
                             user.getNickname().substring(0, 1).toUpperCase() : "C";
            Font font = new Font("Segoe UI", Font.BOLD, size / 2);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int x = (size - fm.stringWidth(initial)) / 2;
            int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(initial, x, y);
            g2d.dispose();
            
            avatar = new ImageIcon(img);
        } else {
            
            Image sourceImage = avatar.getImage();
            Image scaledImage = sourceImage.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            
            BufferedImage circularImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = circularImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(new Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(scaledImage, 0, 0, size, size, null);
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
                        
                        String latestMsg = null;
                        for (int i = lines.size() - 1; i >= 0; i--) {
                            String line = lines.get(i);
                            
                            if (!line.contains(" joined the chat at: ") && 
                                !line.contains(" left the chat at: ") &&
                                !line.contains("Chat started at: ") &&
                                !line.contains("Chat ended at: ")) {
                                latestMsg = line;
                                break;
                            }
                        }
                        
                        if (latestMsg != null) {
                            
                            if (latestMsg.length() > 30) {
                                latestMsg = latestMsg.substring(0, 27) + "...";
                            }
                            messageLabel.setText(latestMsg);
                            
                            
                            try {
                                String timeStr = latestMsg.substring(latestMsg.lastIndexOf(":") - 2, latestMsg.lastIndexOf(":") + 3);
                                timeLabel.setText(timeStr);
                            } catch (Exception e) {
                                
                                timeLabel.setText(TIME_FORMAT.format(chat.getStartTime()));
                            }
                        }
                        
                        
                        int unread = 0;
                        
                        
                        String lastLeavePattern = currentUser.getNickname() + " left the chat at: ";
                        String lastLeaveTime = null;
                        
                        for (int i = lines.size() - 1; i >= 0; i--) {
                            if (lines.get(i).contains(lastLeavePattern)) {
                                lastLeaveTime = lines.get(i).substring(lastLeavePattern.length());
                                break;
                            }
                        }
                        
                        
                        if (lastLeaveTime != null) {
                            for (int i = lines.size() - 1; i >= 0; i--) {
                                String line = lines.get(i);
                                
                                
                                if (!line.contains(" joined the chat at: ") && 
                                    !line.contains(" left the chat at: ") &&
                                    !line.contains("Chat started at: ") &&
                                    !line.contains("Chat ended at: ") &&
                                    !line.startsWith(currentUser.getNickname() + ": ")) {
                                    
                                    
                                    try {
                                        
                                        
                                        int timeIndex = line.lastIndexOf(":");
                                        if (timeIndex > 0 && timeIndex < line.length() - 1) {
                                            String msgTime = line.substring(timeIndex - 8, timeIndex + 3);
                                            if (msgTime.compareTo(lastLeaveTime) > 0) {
                                                unread++;
                                            }
                                        }
                                    } catch (Exception e) {
                                        
                                        unread++;
                                    }
                                }
                            }
                        } else {
                            
                            for (String line : lines) {
                                if (!line.contains(" joined the chat at: ") && 
                                    !line.contains(" left the chat at: ") &&
                                    !line.contains("Chat started at: ") &&
                                    !line.contains("Chat ended at: ") &&
                                    !line.startsWith(currentUser.getNickname() + ": ")) {
                                    unread++;
                                }
                            }
                        }
                        
                        
                        if (unread > 0) {
                            updateUnreadBadge(unread);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading chat info: " + e.getMessage());
                }
            }
        }
    }
    
    private void updateUnreadBadge(int count) {
        this.unreadCount = count;
        
        
        for (Component comp : getComponents()) {
            if (comp == unreadBadge) {
                remove(comp);
                break;
            }
        }
        
        
        unreadBadge = new JLabel(String.valueOf(count));
        unreadBadge.setOpaque(true);
        unreadBadge.setBackground(BADGE_COLOR);
        unreadBadge.setForeground(Color.WHITE);
        unreadBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        unreadBadge.setHorizontalAlignment(SwingConstants.CENTER);
        unreadBadge.setPreferredSize(new Dimension(20, 20));
        unreadBadge.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        
        setLayout(new BorderLayout());
        add(unreadBadge, BorderLayout.EAST);
        
        revalidate();
        repaint();
    }
    
    public Chat getChat() {
        return chat;
    }
} 