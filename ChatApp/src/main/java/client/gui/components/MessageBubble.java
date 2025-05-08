package client.gui.components;

import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageBubble extends JPanel {
    private static final Color MY_MESSAGE_COLOR = new Color(0, 132, 255);
    private static final Color OTHER_MESSAGE_COLOR = new Color(240, 240, 240);
    private static final Color SYSTEM_MESSAGE_COLOR = new Color(230, 230, 230, 150);
    private static final Font MESSAGE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TIME_FONT = new Font("Segoe UI", Font.PLAIN, 10);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    
    private final boolean isCurrentUser;
    private final boolean isSystemMessage;
    private final User sender;
    private final String message;
    private final Date timestamp;
    
    public MessageBubble(User sender, String message, boolean isCurrentUser) {
        this(sender, message, isCurrentUser, false, new Date());
    }
    
    public MessageBubble(User sender, String message, boolean isCurrentUser, boolean isSystemMessage, Date timestamp) {
        this.sender = sender;
        this.message = message;
        this.isCurrentUser = isCurrentUser;
        this.isSystemMessage = isSystemMessage;
        this.timestamp = timestamp;
        
        setOpaque(false);
        setBorder(new EmptyBorder(5, 10, 5, 10));
        setLayout(new BorderLayout(10, 0));
        
        initComponents();
    }
    
    private void initComponents() {
        if (isSystemMessage) {
            createSystemMessage();
        } else {
            createUserMessage();
        }
    }
    
    private void createSystemMessage() {
        // System messages are centered with light gray background
        JLabel systemLabel = new JLabel(message, SwingConstants.CENTER);
        systemLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        systemLabel.setForeground(Color.DARK_GRAY);
        
        JPanel systemPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Paint rounded rectangle background
                g2d.setColor(SYSTEM_MESSAGE_COLOR);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2d.dispose();
            }
        };
        
        systemPanel.setOpaque(false);
        systemPanel.setLayout(new BorderLayout());
        systemPanel.add(systemLabel, BorderLayout.CENTER);
        systemPanel.setBorder(new EmptyBorder(3, 10, 3, 10));
        
        // Center the system message
        JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(systemPanel);
        
        add(wrapperPanel, BorderLayout.CENTER);
    }
    
    private void createUserMessage() {
        // Regular message with avatar and text bubble
        JPanel messageWrapper = new JPanel(new BorderLayout(5, 0));
        messageWrapper.setOpaque(false);
        
        // Create the profile picture
        JLabel avatarLabel = createAvatarLabel();
        
        // Create the message bubble
        JPanel bubblePanel = new JPanel(new BorderLayout(5, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Paint the bubble background
                g2d.setColor(isCurrentUser ? MY_MESSAGE_COLOR : OTHER_MESSAGE_COLOR);
                
                // Rounded rectangle with different radius based on sender
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                
                g2d.dispose();
            }
        };
        bubblePanel.setOpaque(false);
        bubblePanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        // Create message label
        JLabel messageLabel = new JLabel("<html><p style=\"width: 250px\">" + message + "</p></html>");
        messageLabel.setFont(MESSAGE_FONT);
        messageLabel.setForeground(isCurrentUser ? Color.WHITE : Color.BLACK);
        
        // Create time label
        JLabel timeLabel = new JLabel(TIME_FORMAT.format(timestamp));
        timeLabel.setFont(TIME_FONT);
        timeLabel.setForeground(isCurrentUser ? new Color(200, 200, 200) : Color.GRAY);
        
        // Add components to bubble
        bubblePanel.add(messageLabel, BorderLayout.CENTER);
        bubblePanel.add(timeLabel, BorderLayout.SOUTH);
        
        // Layout elements based on sender
        if (isCurrentUser) {
            messageWrapper.add(bubblePanel, BorderLayout.CENTER);
            // Optional: add avatar label here if you want avatar for own messages too
        } else {
            messageWrapper.add(avatarLabel, BorderLayout.WEST);
            messageWrapper.add(bubblePanel, BorderLayout.CENTER);
        }
        
        // Add to parent with proper alignment
        JPanel alignmentPanel = new JPanel();
        alignmentPanel.setOpaque(false);
        alignmentPanel.setLayout(new FlowLayout(isCurrentUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        alignmentPanel.add(messageWrapper);
        
        add(alignmentPanel, BorderLayout.CENTER);
    }
    
    private JLabel createAvatarLabel() {
        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(30, 30));
        
        ImageIcon avatar = null;
        int size = 30;
        
        if (sender != null && sender.getProfilePic() != null && !sender.getProfilePic().isEmpty()) {
            try {
                File imgFile = new File(sender.getProfilePic());
                if (imgFile.exists()) {
                    BufferedImage img = javax.imageio.ImageIO.read(imgFile);
                    if (img != null) {
                        // Create circular avatar
                        BufferedImage circularImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = circularImage.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                        g2.drawImage(img.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
                        g2.dispose();
                        
                        avatar = new ImageIcon(circularImage);
                    }
                }
            } catch (Exception e) {
                // Fall back to default icon
            }
        }
        
        if (avatar == null) {
            // Create default avatar with initial
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Background circle
            g2.setColor(new Color(52, 152, 219)); // Blue
            g2.fillOval(0, 0, size, size);
            
            // Initial letter
            g2.setColor(Color.WHITE);
            String initial = sender != null && sender.getNickname() != null ? 
                            sender.getNickname().substring(0, 1).toUpperCase() : "?";
            Font font = new Font("Segoe UI", Font.BOLD, size / 2);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, (size - fm.stringWidth(initial)) / 2, 
                        (size + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
            
            avatar = new ImageIcon(img);
        }
        
        avatarLabel.setIcon(avatar);
        return avatarLabel;
    }
} 