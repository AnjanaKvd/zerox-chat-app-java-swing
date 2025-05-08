package client.gui;

import client.services.ChatClientImpl;
import model.Chat;
import model.User;
import server.rmi.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel statusLabel;
    private JPanel userListPanel;
    private JScrollPane chatScrollPane;
    
    private final User currentUser;
    private final ChatClientImpl chatClient;
    private final ChatServer chatServer;
    private final ImageIcon userIcon;
    private Chat currentChat;
    
    public ChatWindow(User user, ChatServer server, ChatClientImpl client, Chat chat) {
        this.currentUser = user;
        this.chatServer = server;
        this.chatClient = client;
        this.currentChat = chat;
        this.userIcon = new ImageIcon(createUserIcon(16));
        
        // Set the chat window in the client
        if (client != null) {
            client.setChatWindow(this);
        }
        
        // Setup window
        setTitle("Chat - " + (chat.getAdmin() != null ? chat.getAdmin().getNickname() : "Chat #" + chat.getId()));
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Add window closing handler to leave chat on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitChat();
            }
        });
        
        initComponents();
        layoutComponents();
        
        // Register with the server
        try {
            if (chatServer != null) {
                chatServer.registerClient(chatClient, currentUser.getNickname());
                appendToChatArea("You joined the chat at: " + getCurrentTime());
            } else {
                throw new RemoteException("Chat server is not available");
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, 
                    "Error connecting to chat: " + e.getMessage(), 
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
        
        setVisible(true);
    }
    
    public ChatWindow() {
        // Initialize required fields with null values
        this.currentUser = null;
        this.chatServer = null;
        this.chatClient = null;
        this.userIcon = null;
        
        // Don't perform any UI initialization or server connection
        // This constructor is only for subclassing
    }
    
    private void initComponents() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        chatScrollPane = new JScrollPane(chatArea);
        
        messageField = new JTextField(20);
        messageField.addActionListener(e -> sendMessage());
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        
        statusLabel = new JLabel("Connected as: " + currentUser.getNickname());
        statusLabel.setForeground(Color.BLUE);
        
        userListPanel = new JPanel();
        userListPanel.setBorder(BorderFactory.createTitledBorder("Online Users"));
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Chat area in center
        add(chatScrollPane, BorderLayout.CENTER);
        
        // Message input area at bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(messagePanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // User list on right
        JScrollPane userScrollPane = new JScrollPane(userListPanel);
        userScrollPane.setPreferredSize(new Dimension(200, getHeight()));
        add(userScrollPane, BorderLayout.EAST);
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;
        
        try {
            if (chatServer != null) {
                chatServer.sendMessage(message, currentUser.getNickname());
                
                // If user types "Bye", close the window
                if (message.equalsIgnoreCase("Bye")) {
                    exitChat();
                }
                
                messageField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, 
                        "Chat server is not available", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, 
                    "Error sending message: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exitChat() {
        try {
            if (chatServer != null && chatClient != null) {
                chatServer.removeClient(chatClient, currentUser.getNickname());
            }
            dispose();
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this, 
                    "Error leaving chat: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            // Force dispose even if there's an error
            dispose();
        }
    }
    
    public void appendToChatArea(String message) {
        if (chatArea != null) {
            chatArea.append(message + "\n");
            // Auto-scroll to bottom
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }
    
    public void updateUserList(String[] users) {
        if (userListPanel != null) {
            userListPanel.removeAll();
            
            for (String user : users) {
                JLabel userLabel = new JLabel(user, userIcon, JLabel.LEFT);
                userLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                userListPanel.add(userLabel);
            }
            
            userListPanel.revalidate();
            userListPanel.repaint();
        }
    }
    
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }
    
    // Create a simple user icon
    private Image createUserIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw user icon (simple avatar)
        g2d.setColor(new Color(100, 149, 237)); // Cornflower blue
        g2d.fillOval(0, 0, size, size);
        
        // Draw simplified face
        int headSize = (int)(size * 0.6);
        int headY = (int)(size * 0.15);
        g2d.setColor(new Color(255, 222, 173)); // Navajo white
        g2d.fillOval((size - headSize) / 2, headY, headSize, headSize);
        
        // Draw body
        int bodyWidth = (int)(size * 0.6);
        int bodyHeight = (int)(size * 0.4);
        int bodyX = (size - bodyWidth) / 2;
        int bodyY = (int)(size * 0.7);
        g2d.setColor(new Color(255, 222, 173)); // Navajo white
        g2d.fillRect(bodyX, bodyY, bodyWidth, bodyHeight);
        
        g2d.dispose();
        return image;
    }
} 