package client.gui;

import client.services.ChatClientImpl;
import model.Chat;
import model.User;
import server.rmi.ChatServer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ChatWindow extends JFrame {

    private JTextPane chatArea;
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
        
        
        if (client != null) {
            client.setChatWindow(this);
        }
        
        
        setTitle("Chat - " + (chat.getAdmin() != null ? chat.getAdmin().getNickname() : "Chat #" + chat.getId()));
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitChat();
            }
        });

        initComponents();
        layoutComponents();

        try {
            if (chatServer != null) {
                chatServer.registerClient(chatClient, currentUser.getNickname());
                appendToChatArea("You joined the chat at : " + getCurrentTime());
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
        
        this.currentUser = null;
        this.chatServer = null;
        this.chatClient = null;
        this.userIcon = null;
        
        
        
    }
    
    private void initComponents() {

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(95,158,160));
        chatArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //        chatArea = new JTextArea();
//        chatArea.setEditable(false);
//        chatArea.setLineWrap(true);
//        chatArea.setWrapStyleWord(true);

        chatArea.setBackground(new Color(95,158,160));
        chatArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));


        chatScrollPane = new JScrollPane(chatArea);

        messageField = new JTextField(20);
        messageField.setBounds(5,5,5,5);
        messageField.setBackground(new Color	(248,248,255));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 3, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        messageField.addActionListener(e -> sendMessage());

        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(176, 196, 222));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setFocusPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 105, 217)),
                BorderFactory.createEmptyBorder(8, 82, 8, 82)
        ));
        sendButton.addActionListener(e -> sendMessage());

        statusLabel = new JLabel("Connected as: " + currentUser.getNickname());
        statusLabel.setForeground(Color.BLUE);


        TitledBorder titledBorder = BorderFactory.createTitledBorder("Online Users");
        titledBorder.setTitleColor(Color.WHITE);
        titledBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 16));

        userListPanel = new JPanel();
        userListPanel.setBorder(titledBorder);
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(new Color(0, 128, 128));


        JLabel userLabel = new JLabel(currentUser.getNickname());
        userLabel.setIcon(userIcon);
        userLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        userListPanel.add(userLabel);

    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        
        add(chatScrollPane, BorderLayout.CENTER);
        
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(messagePanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

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
            
            dispose();
        }
    }

    public void appendToChatArea(String message) {
        if (chatArea != null) {
            chatArea.append(message + "\n");
            
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    public void updateUserList(String[] users) {
        if (userListPanel != null) {
            userListPanel.removeAll();
            
            // Use HashSet to filter duplicates (just in case)
            Set<String> uniqueUsers = new HashSet<>(Arrays.asList(users));
            
            for (String user : uniqueUsers) {
                JLabel userLabel = new JLabel(user, userIcon, JLabel.LEFT);
                userLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                userListPanel.add(userLabel);
            }
            
            userListPanel.revalidate();
            userListPanel.repaint();
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        String time = sdf.format(new Date()).toLowerCase();
        return "  "+ time ;
    }
    
    
    private Image createUserIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        
        g2d.setColor(new Color(100, 149, 237)); 
        g2d.fillOval(0, 0, size, size);
        
        int headSize = (int)(size * 0.6);
        int headY = (int)(size * 0.15);
        g2d.setColor(new Color(255, 222, 173)); 
        g2d.fillOval((size - headSize) / 2, headY, headSize, headSize);

        int bodyWidth = (int)(size * 0.6);
        int bodyHeight = (int)(size * 0.4);
        int bodyX = (size - bodyWidth) / 2;
        int bodyY = (int)(size * 0.7);
        g2d.setColor(new Color(255, 222, 173)); 
        g2d.fillRect(bodyX, bodyY, bodyWidth, bodyHeight);

        g2d.dispose();
        return image;
    }
}