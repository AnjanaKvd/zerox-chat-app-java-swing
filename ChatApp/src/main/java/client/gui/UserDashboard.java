package client.gui;

import client.services.ChatClientImpl;
import client.services.ConnectionManager;
import dao.ChatDAO;
import dao.UserDAO;
import model.Chat;
import model.User;
import server.rmi.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.List;

public class UserDashboard extends JFrame {
    private final User currentUser;
    private final UserDAO userDAO;
    private final ChatDAO chatDAO;
    private final ChatServer chatServer;
    private final ChatClientImpl chatClient;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private ChatWindow chatWindow;
    private UserProfilePanel profilePanel;
    private JList<String> availableChatsJList;
    private DefaultListModel<String> chatsListModel;
    
    public UserDashboard(User user) {
        this.currentUser = user;
        this.userDAO = new UserDAO();
        this.chatDAO = new ChatDAO();
        this.chatServer = ConnectionManager.getInstance().getChatServer();
        this.chatClient = ConnectionManager.getInstance().getChatClient();
        
        // Setup window
        setTitle("Chat Application - Welcome " + user.getNickname());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        
        // Handle window closing to clean up resources
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (chatWindow != null) {
                        chatServer.sendMessage("Bye", currentUser.getNickname());
                        chatServer.removeClient(chatClient, currentUser.getNickname());
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        setVisible(true);
    }
    
    private void initComponents() {
        // Create main panel with card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create sidebar
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(200, getHeight()));
        
        // User info panel
        JPanel userInfoPanel = new JPanel(new GridLayout(3, 1));
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getNickname());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userInfoPanel.add(welcomeLabel);
        
        // Navigation buttons
        JPanel navPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        JButton homeButton = new JButton("Home");
        JButton profileButton = new JButton("My Profile");
        JButton chatsButton = new JButton("Available Chats");
        JButton logoutButton = new JButton("Logout");
        
        homeButton.addActionListener(e -> cardLayout.show(mainPanel, "home"));
        profileButton.addActionListener(e -> cardLayout.show(mainPanel, "profile"));
        chatsButton.addActionListener(e -> {
            refreshAvailableChats();
            cardLayout.show(mainPanel, "chats");
        });
        logoutButton.addActionListener(e -> logout());
        
        navPanel.add(homeButton);
        navPanel.add(profileButton);
        navPanel.add(chatsButton);
        navPanel.add(logoutButton);
        
        sidePanel.add(userInfoPanel, BorderLayout.NORTH);
        sidePanel.add(navPanel, BorderLayout.CENTER);
        
        // Create content panels
        JPanel homePanel = createHomePanel();
        profilePanel = new UserProfilePanel(currentUser, userDAO);
        JPanel chatsPanel = createChatsPanel();
        
        // Add panels to card layout
        mainPanel.add(homePanel, "home");
        mainPanel.add(profilePanel, "profile");
        mainPanel.add(chatsPanel, "chats");
        
        // Add components to frame
        setLayout(new BorderLayout());
        add(sidePanel, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
        
        // Show home panel by default
        cardLayout.show(mainPanel, "home");
    }
    
    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Welcome to Chat Application", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(new Font("Arial", Font.PLAIN, 14));
        infoArea.setText("You can:\n\n" +
                "• View and join available chats\n" +
                "• Update your profile information\n" +
                "• Communicate with other users in real-time\n\n" +
                "To start, click on 'Available Chats' in the sidebar and join an active chat.");
        
        infoArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createChatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Available Chats", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Create list model and JList
        chatsListModel = new DefaultListModel<>();
        availableChatsJList = new JList<>(chatsListModel);
        availableChatsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableChatsJList.setCellRenderer(new ChatListCellRenderer());
        
        JScrollPane listScrollPane = new JScrollPane(availableChatsJList);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton joinButton = new JButton("Join Selected Chat");
        JButton refreshButton = new JButton("Refresh List");
        
        joinButton.addActionListener(e -> joinSelectedChat());
        refreshButton.addActionListener(e -> refreshAvailableChats());
        
        buttonsPanel.add(joinButton);
        buttonsPanel.add(refreshButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(listScrollPane, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);
        
        // Initial load of chats
        refreshAvailableChats();
        
        return panel;
    }
    
    private void refreshAvailableChats() {
        try {
            // Get active chats from database
            List<Chat> chats = chatDAO.getActiveChats();
            chatsListModel.clear();
            
            if (chats.isEmpty()) {
                chatsListModel.addElement("No active chats available");
            } else {
                for (Chat chat : chats) {
                    chatsListModel.addElement("Chat #" + chat.getId() + " (Started: " + chat.getStartTime() + ")");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading chats: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void joinSelectedChat() {
        // First, check if we're connected to the server
        if (chatServer == null || chatClient == null) {
            JOptionPane.showMessageDialog(this,
                    "Cannot join chat: Not connected to server",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int selectedIndex = availableChatsJList.getSelectedIndex();
        if (selectedIndex == -1 || chatsListModel.isEmpty() || 
            chatsListModel.get(0).equals("No active chats available")) {
            JOptionPane.showMessageDialog(this,
                    "Please select an active chat to join",
                    "No Chat Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Parse chat ID from the selected item
        String selectedItem = chatsListModel.get(selectedIndex);
        int chatId;
        try {
            chatId = Integer.parseInt(selectedItem.substring(6, selectedItem.indexOf(" (Started")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error parsing chat ID: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Create chat window and join the chat
            chatWindow = new ChatWindow(currentUser, chatServer, chatClient);
            chatWindow.setVisible(true);
            
            // Minimize this window
            setState(JFrame.ICONIFIED);
        } catch (Exception e) {
            e.printStackTrace(); // Print stack trace for debugging
            JOptionPane.showMessageDialog(this,
                    "Error joining chat: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void logout() {
        try {
            // If in a chat, leave it properly
            if (chatWindow != null) {
                chatServer.sendMessage("Bye", currentUser.getNickname());
                chatServer.removeClient(chatClient, currentUser.getNickname());
            }
            
            // Go back to login screen
            dispose();
            new LoginForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error during logout: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Create a simple chat icon
    private Image createChatIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a chat bubble
        int padding = size / 10;
        g2d.setColor(new Color(70, 130, 180)); // Steel blue
        g2d.fillRoundRect(padding, padding, size - 2*padding, size - 2*padding, 10, 10);
        
        // Draw text-line indicators
        g2d.setColor(Color.WHITE);
        int lineHeight = size / 10;
        int lineWidth = size / 2;
        int x = size / 4;
        int y1 = size / 3;
        int y2 = size / 2;
        int y3 = 2 * size / 3;
        
        g2d.fillRect(x, y1, lineWidth, lineHeight);
        g2d.fillRect(x, y2, lineWidth, lineHeight);
        g2d.fillRect(x, y3, lineWidth, lineHeight);
        
        g2d.dispose();
        return image;
    }
    
    // Custom cell renderer for the chats list
    private class ChatListCellRenderer extends DefaultListCellRenderer {
        private final ImageIcon chatIcon;
        
        public ChatListCellRenderer() {
            // Create the icon once when the renderer is instantiated
            chatIcon = new ImageIcon(createChatIcon(16));
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            
            // Use the programmatically created icon
            label.setIcon(chatIcon);
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            return label;
        }
    }
} 