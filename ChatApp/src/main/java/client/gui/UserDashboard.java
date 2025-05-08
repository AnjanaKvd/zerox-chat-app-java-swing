package client.gui;

import client.gui.components.ChatCard;
import client.services.ChatClientImpl;
import client.services.ConnectionManager;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dao.ChatDAO;
import dao.UserDAO;
import model.Chat;
import model.User;
import server.rmi.ChatServer;
import dao.MessageDAO;
import client.gui.components.ChatPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.LineBorder;
import java.awt.AlphaComposite;
import java.io.File;
import java.awt.event.MouseListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class UserDashboard extends JFrame {
    private final User currentUser;
    private final UserDAO userDAO;
    private final ChatDAO chatDAO;
    private final ChatServer chatServer;
    private ChatClientImpl chatClient;

    private JPanel homeTabPanel;
    private JPanel allChatsTabPanel;
    private JCheckBox themeSwitcherCheckBox;
    private JLabel profileImageLabel;

    private static boolean isDarkMode = false; // To persist theme choice within session

    // Colors & Fonts (can be centralized later)
    private final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private final Color RED_COLOR = new Color(211, 47, 47);
    private final Color BUTTON_TEXT_COLOR = Color.WHITE;
    private final Font GENERAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);

    // Add these class variables
    private JPanel chatListPanel;
    private JPanel activeChatPanel;
    private boolean isChatActive = false;
    private Chat currentChat;
    private JTextArea chatArea;
    private JTextField messageField;
    private ChatPanel chatPanel;
    private JPanel onlineUsersPanel;

    public UserDashboard(User user) {
        this.currentUser = user;
        this.userDAO = new UserDAO();
        this.chatDAO = new ChatDAO();
        this.chatServer = ConnectionManager.getInstance().getChatServer();
        this.chatClient = ConnectionManager.getInstance().getChatClient();

        // Apply initial theme
        applyTheme();

        setTitle("Chat Application - " + currentUser.getNickname());
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle closing manually
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });

        setVisible(true);
        loadSubscribedChats();
        loadAllChats();
    }

    private void applyTheme() {
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 8);
             // Custom focus color (optional)
            UIManager.put("Component.focusColor", PRIMARY_COLOR.brighter());
            UIManager.put("Component.focusedBorderColor", PRIMARY_COLOR);
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to set FlatLaf theme: " + ex.getMessage());
        }
    }

    private void initComponents() {
        // Top Bar Panel
        JPanel topBarPanel = createTopBar();
        add(topBarPanel, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(GENERAL_FONT);

        homeTabPanel = createHomeTabPanel();
        allChatsTabPanel = createAllChatsTabPanel();

        tabbedPane.addTab("My Chats", homeTabPanel);
        tabbedPane.addTab("Discover Chats", allChatsTabPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(10, 15, 10, 15));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        // Left side: Profile Pic
        profileImageLabel = new JLabel();
        profileImageLabel.setPreferredSize(new Dimension(45, 45)); // Slightly larger for better visibility
        profileImageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileImageLabel.setToolTipText("View Profile");
        loadAndSetProfileImage(); // Call this to draw the initial icon

        profileImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UserProfilePanel profilePanel = new UserProfilePanel(currentUser, userDAO);
                JDialog dialog = new JDialog(UserDashboard.this, "User Profile", true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setContentPane(profilePanel);
                dialog.pack(); // Pack after adding content
                dialog.setMinimumSize(new Dimension(400, 500)); // Ensure a reasonable minimum size
                dialog.setLocationRelativeTo(UserDashboard.this);
                dialog.setVisible(true);
            }
        });
        
        JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topLeftPanel.setOpaque(false);
        topLeftPanel.add(profileImageLabel);

        // Right side: Theme Switcher and Logout Button
        themeSwitcherCheckBox = new JCheckBox(isDarkMode ? "Light" : "Dark");
        themeSwitcherCheckBox.setFont(GENERAL_FONT);
        themeSwitcherCheckBox.setSelected(isDarkMode);
        themeSwitcherCheckBox.putClientProperty("FlatLaf.styleClass", "switch"); // Style as a switch
        themeSwitcherCheckBox.setOpaque(false);
        themeSwitcherCheckBox.setToolTipText("Toggle Dark/Light Mode");
        themeSwitcherCheckBox.addActionListener(e -> switchTheme());

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(BOLD_FONT);
        logoutButton.setBackground(RED_COLOR);
        logoutButton.setForeground(BUTTON_TEXT_COLOR);
        logoutButton.setFocusPainted(false);
        logoutButton.setPreferredSize(new Dimension(100, 35));
        logoutButton.addActionListener(e -> handleLogout());

        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRightPanel.setOpaque(false);
        topRightPanel.add(themeSwitcherCheckBox);
        topRightPanel.add(logoutButton);

        topBar.add(topLeftPanel, BorderLayout.WEST);
        topBar.add(topRightPanel, BorderLayout.EAST);
        return topBar;
    }
    
    private void loadAndSetProfileImage() {
        String imagePath = currentUser.getProfilePic();
        int diameter = 40;
        
        // Create a default icon first (will be used if no profile pic is found)
        BufferedImage defaultImg = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = defaultImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill circle with primary color
        g2.setColor(PRIMARY_COLOR);
        g2.fillOval(0, 0, diameter, diameter);
        
        // Draw the initial letter
        g2.setColor(Color.WHITE);
        String initial = currentUser.getNickname() != null ? 
                         currentUser.getNickname().substring(0, 1).toUpperCase() : "U";
        g2.setFont(new Font("Segoe UI", Font.BOLD, diameter / 2));
        FontMetrics fm = g2.getFontMetrics();
        int x = (diameter - fm.stringWidth(initial)) / 2;
        int y = ((diameter - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(initial, x, y);
        g2.dispose();
        
        ImageIcon profileIcon = new ImageIcon(defaultImg);
        
        // Try to load actual profile image if exists
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists() && imgFile.isFile()) {
                try {
                    // Load image with ImageIO for better error handling
                    BufferedImage img = javax.imageio.ImageIO.read(imgFile);
                    if (img != null) {
                        // Create circular mask
                        BufferedImage circularImg = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = circularImg.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setClip(new Ellipse2D.Float(0, 0, diameter, diameter));
                        
                        // Draw scaled image
                        g.drawImage(img.getScaledInstance(diameter, diameter, Image.SCALE_SMOOTH), 0, 0, null);
                        g.dispose();
                        
                        profileIcon = new ImageIcon(circularImg);
                        System.out.println("Successfully loaded profile image: " + imagePath);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading profile image: " + e.getMessage());
                    // Will use default icon created above
                }
            } else {
                System.err.println("Profile image file not found: " + imagePath);
            }
        }
        
        // Set the icon
        profileImageLabel.setIcon(profileIcon);
        profileImageLabel.repaint();
    }

    private void switchTheme() {
        isDarkMode = !isDarkMode;
        themeSwitcherCheckBox.setSelected(isDarkMode);
        themeSwitcherCheckBox.setText(isDarkMode ? "Light" : "Dark");
        applyTheme();
        SwingUtilities.updateComponentTreeUI(this);
        // Force redraw of custom painted components if any
        if (profileImageLabel != null) loadAndSetProfileImage(); 
    }

    private void handleLogout() {
        try {
            if (chatServer != null && chatClient != null && currentUser != null) {
                // This assumes a generic "Bye" message on logout.
                // Specific chat window closing should handle leaving that particular chat.
                // If the user is in a chat, they should ideally send "Bye" from ChatWindow
                // This is a more general disconnect.
            }
        } catch (Exception ex) {
            System.err.println("Error during logout cleanup: " + ex.getMessage());
        } finally {
            dispose();
            new LoginForm();
        }
    }

    private JPanel createHomeTabPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(new JLabel("Subscribed chats will appear here.", SwingConstants.CENTER)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAllChatsTabPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(new JLabel("All available chats will appear here.", SwingConstants.CENTER)), BorderLayout.CENTER);
        return panel;
    }
    
    private void loadSubscribedChats() {
        if (homeTabPanel == null) homeTabPanel = createHomeTabPanel();
        homeTabPanel.removeAll();

        List<Chat> subscribedChats = chatDAO.getSubscribedChats(currentUser.getId());
        if (subscribedChats == null) subscribedChats = new ArrayList<>();

        if (subscribedChats.isEmpty()) {
            JLabel noChatsLabel = new JLabel("You are not subscribed to any chats yet. Visit 'Discover Chats'.");
            noChatsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noChatsLabel.setFont(GENERAL_FONT);
            homeTabPanel.add(noChatsLabel, BorderLayout.CENTER);
        } else {
            JPanel chatListPanel = new JPanel();
            chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
            chatListPanel.setBackground(Color.WHITE);
            
            for (Chat chat : subscribedChats) {
                ChatCard chatCard = new ChatCard(chat, currentUser);
                chatCard.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        openChatWindow(chat);
                    }
                });
                chatListPanel.add(chatCard);
            }
            
            // Add some padding at the bottom
            chatListPanel.add(Box.createVerticalStrut(20));
            
            JScrollPane scrollPane = new JScrollPane(chatListPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            homeTabPanel.add(scrollPane, BorderLayout.CENTER);
        }
        
        homeTabPanel.revalidate();
        homeTabPanel.repaint();
    }

    private void openChatWindow(Chat chat) {
        try {
            this.currentChat = chat;
            this.isChatActive = true;
            
            // Clear the homeTabPanel and set up for chat display
            homeTabPanel.removeAll();
            
            // Create the chat interface
            setupChatInterface();
            
            // Load chat history from log file
            loadChatHistory();
            
            // Register with chat server
            ChatServer chatServer = ConnectionManager.getInstance().getChatServer();
            if (chatServer != null) {
                try {
                    // Create a simplified ChatWindow for callback
                    ChatWindow chatWindowCallback = new ChatWindow() {
                        @Override
                        public void appendToChatArea(String message) {
                            UserDashboard.this.appendToChatArea(message);
                            // Also log the message to file
                            appendToLogFile(message);
                        }
                        
                        @Override
                        public void updateUserList(String[] users) {
                            // Can implement this later if needed
                        }
                    };
                    
                    // Create new client for this chat
                    this.chatClient = new ChatClientImpl() {
                        @Override
                        public void receiveMessage(String message) throws RemoteException {
                            appendToChatArea(message);
                            super.receiveMessage(message);
                        }
                        
                        @Override
                        public void updateUserList(String[] users) throws RemoteException {
                            UserDashboard.this.updateUserList(users);
                            super.updateUserList(users);
                        }
                        
                        @Override
                        public void notifyChatStarted(String time) throws RemoteException {
                            appendToChatArea("Chat started at: " + time);
                            super.notifyChatStarted(time);
                        }
                        
                        @Override
                        public void notifyChatEnded(String time) throws RemoteException {
                            appendToChatArea("Chat ended at: " + time);
                            super.notifyChatEnded(time);
                        }
                    };
                    
                    this.chatClient.setChatWindow(chatWindowCallback);
                    
                    chatServer.registerClient(this.chatClient, currentUser.getNickname());
                    
                    // Log user joined event
                    String joinMessage = currentUser.getNickname() + " joined the chat at: " + getCurrentTime();
                    appendToChatArea(joinMessage);
                    appendToLogFile(joinMessage);
                } catch (RemoteException e) {
                    throw new Exception("Failed to register with chat server: " + e.getMessage());
                }
            } else {
                throw new Exception("Chat server is not available");
            }
            
            homeTabPanel.revalidate();
            homeTabPanel.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error opening chat: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            
            // If error, go back to chat list
            goBackToChats();
        }
    }

    private void setupChatInterface() {
        // Top panel with back button and chat info
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JButton backButton = new JButton("← Back to Chats");
        backButton.setFont(GENERAL_FONT);
        backButton.addActionListener(e -> goBackToChats());
        
        JLabel chatTitleLabel = new JLabel();
        User admin = currentChat.getAdmin();
        String chatName = (admin != null) ? admin.getUsername() + "'s Chat" : "Chat #" + currentChat.getId();
        chatTitleLabel.setText(chatName);
        chatTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(chatTitleLabel, BorderLayout.CENTER);
        
        // Create the main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // Chat panel (center)
        chatPanel = new ChatPanel(currentUser);
        
        // Online users panel (right)
        onlineUsersPanel = new JPanel();
        onlineUsersPanel.setLayout(new BoxLayout(onlineUsersPanel, BoxLayout.Y_AXIS));
        onlineUsersPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5), 
            "Online Users", 
            0, 
            0, 
            new Font("Segoe UI", Font.BOLD, 12), 
            Color.DARK_GRAY
        ));
        onlineUsersPanel.setBackground(Color.WHITE);
        onlineUsersPanel.setPreferredSize(new Dimension(180, 0));
        
        // Add placeholder text for online users
        JLabel waitingLabel = new JLabel("Waiting for users...", SwingConstants.CENTER);
        waitingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        waitingLabel.setFont(GENERAL_FONT);
        waitingLabel.setForeground(Color.GRAY);
        onlineUsersPanel.add(Box.createVerticalStrut(10));
        onlineUsersPanel.add(waitingLabel);
        
        // Add chat and users to content panel
        contentPanel.add(chatPanel, BorderLayout.CENTER);
        contentPanel.add(new JScrollPane(onlineUsersPanel), BorderLayout.EAST);
        
        // Message input area (bottom)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setFont(GENERAL_FONT);
        messageField.addActionListener(e -> sendMessage());
        
        JButton sendButton = new JButton("Send");
        sendButton.setFont(BOLD_FONT);
        sendButton.setBackground(PRIMARY_COLOR);
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> sendMessage());
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // Add components to main panel
        homeTabPanel.setLayout(new BorderLayout());
        homeTabPanel.add(topPanel, BorderLayout.NORTH);
        homeTabPanel.add(contentPanel, BorderLayout.CENTER);
        homeTabPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void goBackToChats() {
        // Log user left event
        if (isChatActive && currentChat != null) {
            try {
                String leaveMessage = currentUser.getNickname() + " left the chat at: " + getCurrentTime();
                appendToChatArea(leaveMessage);
                appendToLogFile(leaveMessage);
                
                // Unregister from chat server
                if (chatClient != null) {
                    ChatServer chatServer = ConnectionManager.getInstance().getChatServer();
                    if (chatServer != null) {
                        chatServer.removeClient(chatClient, currentUser.getNickname());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error leaving chat: " + e.getMessage());
            }
        }
        
        // Reset chat state
        this.isChatActive = false;
        this.currentChat = null;
        this.chatClient = null;
        
        // Reload the chats list
        loadSubscribedChats();
    }

    // Add these methods to handle chat functionality
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;
        
        try {
            ChatServer chatServer = ConnectionManager.getInstance().getChatServer();
            if (chatServer != null) {
                // Add timestamp to the message
                String timestampedMessage = message + " [" + 
                       new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]";
                
                chatServer.sendMessage(timestampedMessage, currentUser.getNickname());
                messageField.setText("");
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, 
                    "Error sending message: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void appendToChatArea(String message) {
        if (chatPanel != null) {
            chatPanel.addMessage(message);
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void loadAllChats() {
        if (allChatsTabPanel == null) allChatsTabPanel = createAllChatsTabPanel();
        allChatsTabPanel.removeAll();

        List<Chat> allChats = chatDAO.getActiveChats();
         if (allChats == null) allChats = new ArrayList<>();

        if (allChats.isEmpty()) {
            JLabel noChatsLabel = new JLabel("No chats available at the moment.");
            noChatsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noChatsLabel.setFont(GENERAL_FONT);
            allChatsTabPanel.add(noChatsLabel, BorderLayout.CENTER);
        } else {
            JPanel chatListPanel = new JPanel();
            chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
            chatListPanel.setBackground(Color.WHITE);
            
            // Get user's subscribed chat IDs
            List<Integer> subscribedChatIds = new ArrayList<>();
            List<Chat> userSubscribedChats = chatDAO.getSubscribedChats(currentUser.getId());
            if (userSubscribedChats != null) {
                for (Chat c : userSubscribedChats) subscribedChatIds.add(c.getId());
            }

            for (Chat chat : allChats) {
                boolean isSubscribed = subscribedChatIds.contains(chat.getId());
                
                JPanel chatEntry = new JPanel(new BorderLayout());
                chatEntry.setBackground(Color.WHITE);
                chatEntry.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
                
                // Chat info
                JPanel chatInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                chatInfoPanel.setOpaque(false);
                
                // Admin's profile pic
                JLabel avatarLabel = new JLabel();
                avatarLabel.setPreferredSize(new Dimension(32, 32));
                User admin = chat.getAdmin();
                // Set avatar (reusing logic from ChatCard - could be extracted to a utility)
                if (admin != null && admin.getProfilePic() != null) {
                    try {
                        ImageIcon icon = new ImageIcon(admin.getProfilePic());
                        avatarLabel.setIcon(icon);
                    } catch (Exception e) {
                        // Default avatar will be set below
                    }
                }
                
                // If no avatar was set, use default
                if (avatarLabel.getIcon() == null) {
                    // Simple colored circle with initial
                    BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    g2d.setColor(new Color(52, 152, 219));
                    g2d.fillOval(0, 0, 32, 32);
                    
                    g2d.setColor(Color.WHITE);
                    String initial = admin != null && admin.getNickname() != null ? 
                                    admin.getNickname().substring(0, 1).toUpperCase() : "C";
                    Font font = new Font("Segoe UI", Font.BOLD, 16);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (32 - fm.stringWidth(initial)) / 2;
                    int y = ((32 - fm.getHeight()) / 2) + fm.getAscent();
                    g2d.drawString(initial, x, y);
                    g2d.dispose();
                    
                    avatarLabel.setIcon(new ImageIcon(img));
                }
                
                JLabel chatNameLabel = new JLabel();
                if (admin != null) {
                    chatNameLabel.setText(admin.getUsername() + "'s Chat");
                } else {
                    chatNameLabel.setText("Chat #" + chat.getId());
                }
                chatNameLabel.setFont(GENERAL_FONT);
                
                chatInfoPanel.add(avatarLabel);
                chatInfoPanel.add(Box.createHorizontalStrut(10));
                chatInfoPanel.add(chatNameLabel);
                
                // Subscribe button
                JButton subscribeButton = new JButton(isSubscribed ? "Unsubscribe" : "Subscribe");
                subscribeButton.setFont(GENERAL_FONT);
                
                if (isSubscribed) {
                    subscribeButton.setBackground(new Color(211, 47, 47)); // Red color for unsubscribe
                    subscribeButton.setForeground(Color.WHITE);
                } else {
                    subscribeButton.setBackground(PRIMARY_COLOR); // Blue color for subscribe  
                    subscribeButton.setForeground(Color.WHITE);
                }
                
                subscribeButton.setFocusPainted(false);
                subscribeButton.addActionListener(e -> handleSubscribeToggle(chat, subscribeButton, isSubscribed));
                
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.setOpaque(false);
                buttonPanel.add(subscribeButton);
                
                chatEntry.add(chatInfoPanel, BorderLayout.WEST);
                chatEntry.add(buttonPanel, BorderLayout.EAST);
                
                chatListPanel.add(chatEntry);
            }
            
            // Add some padding at the bottom
            chatListPanel.add(Box.createVerticalStrut(20));
            
            JScrollPane scrollPane = new JScrollPane(chatListPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            allChatsTabPanel.add(scrollPane, BorderLayout.CENTER);
        }
        
        allChatsTabPanel.revalidate();
        allChatsTabPanel.repaint();
    }

    private void handleSubscribeToggle(Chat chat, JButton button, boolean isCurrentlySubscribed) {
        try {
            if (isCurrentlySubscribed) {
                // Unsubscribe
                chatDAO.unsubscribeUserFromChat(currentUser.getId(), chat.getId());
                button.setText("Subscribe");
                button.setBackground(PRIMARY_COLOR);
            } else {
                // Subscribe
            chatDAO.subscribeUserToChat(currentUser.getId(), chat.getId());
                button.setText("Unsubscribe");
                button.setBackground(new Color(211, 47, 47));
            }
            
            // Refresh subscribed chats list
            loadSubscribedChats();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                    "Error " + (isCurrentlySubscribed ? "unsubscribing from" : "subscribing to") + 
                    " chat: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadChatHistory() {
        String logFile = currentChat.getLogFile();
        if (logFile != null && !logFile.isEmpty()) {
            try {
                File file = new File(logFile);
                if (file.exists()) {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        chatPanel.addMessage(line);
                    }
                    System.out.println("Loaded chat history from: " + logFile);
                }
            } catch (Exception e) {
                System.err.println("Error loading chat history: " + e.getMessage());
            }
        }
    }

    private void appendToLogFile(String message) {
        String logFile = currentChat.getLogFile();
        if (logFile == null || logFile.isEmpty()) {
            // Create new log file if none exists
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            logFile = "chat_" + currentChat.getId() + "_" + timestamp + ".txt";
            
            // Update chat record
            try {
                currentChat.setLogFile(logFile);
                chatDAO.saveChat(currentChat);
            } catch (Exception e) {
                System.err.println("Error updating chat log file: " + e.getMessage());
            }
        }
        
        // Append to log file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(message);
            writer.newLine();
        } catch (Exception e) {
            System.err.println("Error writing to chat log: " + e.getMessage());
        }
    }

    // Add a method to update the online users list
    public void updateUserList(String[] users) {
        if (onlineUsersPanel != null) {
            onlineUsersPanel.removeAll();
            onlineUsersPanel.add(Box.createVerticalStrut(10));
            
            if (users == null || users.length == 0) {
                JLabel noUsersLabel = new JLabel("No users online", SwingConstants.CENTER);
                noUsersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                noUsersLabel.setFont(GENERAL_FONT);
                noUsersLabel.setForeground(Color.GRAY);
                onlineUsersPanel.add(noUsersLabel);
            } else {
                for (String username : users) {
                    JPanel userPanel = createOnlineUserPanel(username);
                    onlineUsersPanel.add(userPanel);
                    onlineUsersPanel.add(Box.createVerticalStrut(5));
                }
            }
            
            onlineUsersPanel.revalidate();
            onlineUsersPanel.repaint();
        }
    }

    // Create an online user panel with profile image
    private JPanel createOnlineUserPanel(String username) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Create avatar
        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(30, 30));
        
        // Find the User object based on username (simplistic approach)
        User user = null;
        try {
            // You might want to use a UserDAO method to find by username
            // This is a simplistic approach assuming currentUser might match
            if (currentUser.getUsername().equals(username) || 
                currentUser.getNickname().equals(username)) {
                user = currentUser;
            }
        } catch (Exception e) {
            // Ignore, will use default icon
        }
        
        if (user == null) {
            // Create user with just the username/nickname for avatar generation
            user = new User();
            user.setNickname(username);
        }
        
        // Set avatar (reuse logic from existing methods)
        setUserAvatar(avatarLabel, user, 30);
        
        // Username label
        JLabel usernameLabel = new JLabel(username);
        usernameLabel.setFont(GENERAL_FONT);
        
        // Status indicator (green dot for online)
        JPanel statusDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(76, 175, 80));
                g2d.fillOval(0, 0, 8, 8);
                g2d.dispose();
            }
        };
        statusDot.setOpaque(false);
        statusDot.setPreferredSize(new Dimension(8, 8));
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(statusDot);
        
        panel.add(avatarLabel, BorderLayout.WEST);
        panel.add(usernameLabel, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.EAST);
        
        return panel;
    }

    // Helper method to set user avatar
    private void setUserAvatar(JLabel avatarLabel, User user, int size) {
        ImageIcon avatar = null;
        
        if (user != null && user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
            try {
                File imgFile = new File(user.getProfilePic());
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
            String initial = user != null && user.getNickname() != null ? 
                            user.getNickname().substring(0, 1).toUpperCase() : "?";
            Font font = new Font("Segoe UI", Font.BOLD, size / 2);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, (size - fm.stringWidth(initial)) / 2, 
                        (size + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
            
            avatar = new ImageIcon(img);
        }
        
        avatarLabel.setIcon(avatar);
    }

    // The old ChatWindow related logic like `joinSelectedChat` from previous UserDashboard
    // will need to be re-integrated when a user clicks on a chat from "My Chats" tab.
    // The ChatListCellRenderer and createChatIcon might be useful for "My Chats" tab later.
} 