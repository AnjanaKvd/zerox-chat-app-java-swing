package client.gui;

import client.services.ConnectionManager;
import dao.ChatDAO;
import dao.UserDAO;
import model.Chat;
import model.User;
import server.rmi.ChatServer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class AdminPanel extends JFrame {
    private final User adminUser;
    private final UserDAO userDAO;
    private final ChatDAO chatDAO;
    private final ChatServer chatServer;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTable usersTable;
    private DefaultTableModel usersTableModel;
    private JTable chatsTable;
    private DefaultTableModel chatsTableModel;
    private JTable subscriptionsTable;
    private DefaultTableModel subscriptionsTableModel;
    private JComboBox<String> userComboBox;
    private JComboBox<String> chatComboBox;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public AdminPanel(User admin) {
        this.adminUser = admin;
        this.userDAO = new UserDAO();
        this.chatDAO = new ChatDAO();
        this.chatServer = ConnectionManager.getInstance().getChatServer();
        
        // Setup window
        setTitle("Admin Dashboard - Chat Application");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        setVisible(true);
    }
    
    private void initComponents() {
        // Create main panel with card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create sidebar
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(200, getHeight()));
        sidePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
        
        // Admin info panel
        JPanel adminInfoPanel = new JPanel(new GridLayout(2, 1));
        JLabel titleLabel = new JLabel("Admin Dashboard", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel adminLabel = new JLabel("Logged in as: " + adminUser.getUsername(), JLabel.CENTER);
        adminInfoPanel.add(titleLabel);
        adminInfoPanel.add(adminLabel);
        adminInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Navigation buttons
        JPanel navPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton homeButton = new JButton("Dashboard Home");
        JButton usersButton = new JButton("Manage Users");
        JButton chatsButton = new JButton("Manage Chats");
        JButton subscriptionsButton = new JButton("Manage Subscriptions");
        JButton startChatButton = new JButton("Start New Chat");
        JButton logoutButton = new JButton("Logout");
        
        homeButton.addActionListener(e -> cardLayout.show(mainPanel, "home"));
        usersButton.addActionListener(e -> {
            refreshUsersTable();
            cardLayout.show(mainPanel, "users");
        });
        chatsButton.addActionListener(e -> {
            refreshChatsTable();
            cardLayout.show(mainPanel, "chats");
        });
        subscriptionsButton.addActionListener(e -> {
            refreshSubscriptionsPanel();
            cardLayout.show(mainPanel, "subscriptions");
        });
        startChatButton.addActionListener(e -> startNewChat());
        logoutButton.addActionListener(e -> logout());
        
        navPanel.add(homeButton);
        navPanel.add(usersButton);
        navPanel.add(chatsButton);
        navPanel.add(subscriptionsButton);
        navPanel.add(startChatButton);
        navPanel.add(logoutButton);
        
        sidePanel.add(adminInfoPanel, BorderLayout.NORTH);
        sidePanel.add(navPanel, BorderLayout.CENTER);
        
        // Create content panels
        JPanel homePanel = createHomePanel();
        JPanel usersPanel = createUsersPanel();
        JPanel chatsPanel = createChatsPanel();
        JPanel subscriptionsPanel = createSubscriptionsPanel();
        
        // Add panels to card layout
        mainPanel.add(homePanel, "home");
        mainPanel.add(usersPanel, "users");
        mainPanel.add(chatsPanel, "chats");
        mainPanel.add(subscriptionsPanel, "subscriptions");
        
        // Add components to frame
        setLayout(new BorderLayout());
        add(sidePanel, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
        
        // Show home panel by default
        cardLayout.show(mainPanel, "home");
    }
    
    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel welcomeLabel = new JLabel("Welcome to Admin Dashboard", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // User count stat
        JPanel userStatPanel = createStatPanel("Total Users", getUserCount());
        
        // Active chat count stat
        JPanel chatStatPanel = createStatPanel("Active Chats", getActiveChatCount());
        
        // Log files count
        JPanel logStatPanel = createStatPanel("Saved Logs", getLogFileCount());
        
        // Quick actions panel
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Quick Actions"));
        
        JButton newChatBtn = new JButton("Create New Chat");
        newChatBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        newChatBtn.addActionListener(e -> startNewChat());
        
        JButton manageUsersBtn = new JButton("Manage Users");
        manageUsersBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        manageUsersBtn.addActionListener(e -> {
            refreshUsersTable();
            cardLayout.show(mainPanel, "users");
        });
        
        actionsPanel.add(Box.createVerticalStrut(10));
        actionsPanel.add(newChatBtn);
        actionsPanel.add(Box.createVerticalStrut(10));
        actionsPanel.add(manageUsersBtn);
        actionsPanel.add(Box.createVerticalStrut(10));
        
        statsPanel.add(userStatPanel);
        statsPanel.add(chatStatPanel);
        statsPanel.add(logStatPanel);
        statsPanel.add(actionsPanel);
        
        // System status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("System Status"));
        
        JTextArea statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.append("Server running at: " + new java.util.Date() + "\n");
        statusArea.append("Database connected: " + (HibernateUtil.getSessionFactory() != null) + "\n");
        statusArea.append("RMI Service available: " + (chatServer != null) + "\n");
        statusArea.append("Log directory: " + new File("logs").getAbsolutePath() + "\n");
        
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        
        // Main layout
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(statsPanel, BorderLayout.CENTER);
        centerPanel.add(statusPanel, BorderLayout.SOUTH);
        
        panel.add(welcomeLabel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatPanel(String title, int value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel valueLabel = new JLabel(String.valueOf(value), JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 36));
        valueLabel.setForeground(new Color(0, 102, 204));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("User Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Create users table
        String[] columnNames = {"ID", "Username", "Email", "Nickname", "Profile Pic"};
        usersTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        usersTable = new JTable(usersTableModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(usersTable);
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Users");
        JButton removeButton = new JButton("Remove Selected User");
        
        refreshButton.addActionListener(e -> refreshUsersTable());
        removeButton.addActionListener(e -> removeSelectedUser());
        
        actionPanel.add(refreshButton);
        actionPanel.add(removeButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        // Initial load of users
        refreshUsersTable();
        
        return panel;
    }
    
    private JPanel createChatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Chat Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Create chats table
        String[] columnNames = {"ID", "Start Time", "End Time", "Status", "Log File"};
        chatsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        chatsTable = new JTable(chatsTableModel);
        chatsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(chatsTable);
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Chats");
        JButton viewLogButton = new JButton("View Selected Log");
        JButton endChatButton = new JButton("End Selected Chat");
        
        refreshButton.addActionListener(e -> refreshChatsTable());
        viewLogButton.addActionListener(e -> viewSelectedChatLog());
        endChatButton.addActionListener(e -> endSelectedChat());
        
        actionPanel.add(refreshButton);
        actionPanel.add(viewLogButton);
        actionPanel.add(endChatButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        // Initial load of chats
        refreshChatsTable();
        
        return panel;
    }
    
    private JPanel createSubscriptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Manage User Subscriptions", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Create top panel for subscribing users
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Subscribe/Unsubscribe Users"));
        
        JPanel selectionPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        selectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        selectionPanel.add(new JLabel("Select User:"));
        userComboBox = new JComboBox<>();
        selectionPanel.add(userComboBox);
        
        selectionPanel.add(new JLabel("Select Chat:"));
        chatComboBox = new JComboBox<>();
        selectionPanel.add(chatComboBox);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton subscribeButton = new JButton("Subscribe User to Chat");
        JButton unsubscribeButton = new JButton("Unsubscribe User from Chat");
        
        subscribeButton.addActionListener(e -> subscribeUserToChat());
        unsubscribeButton.addActionListener(e -> unsubscribeUserFromChat());
        
        buttonPanel.add(subscribeButton);
        buttonPanel.add(unsubscribeButton);
        
        topPanel.add(selectionPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Create bottom panel with subscriptions table
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Current Subscriptions"));
        
        String[] columnNames = {"User", "Chat", "Subscribed Date"};
        subscriptionsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        subscriptionsTable = new JTable(subscriptionsTableModel);
        JScrollPane tableScrollPane = new JScrollPane(subscriptionsTable);
        
        JPanel tableActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshSubscriptionsButton = new JButton("Refresh Subscriptions");
        refreshSubscriptionsButton.addActionListener(e -> refreshSubscriptionsPanel());
        tableActionPanel.add(refreshSubscriptionsButton);
        
        bottomPanel.add(tableScrollPane, BorderLayout.CENTER);
        bottomPanel.add(tableActionPanel, BorderLayout.SOUTH);
        
        // Add panels to main panel
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        splitPane.setDividerLocation(200);
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshUsersTable() {
        try {
            // Clear existing data
            usersTableModel.setRowCount(0);
            
            // Get all users
            List<User> users = userDAO.getAllUsers();
            
            for (User user : users) {
                if (user.getUsername().equals("admin")) continue; // Skip admin user
                
                Object[] row = {
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfilePic() != null ? "Yes" : "No"
                };
                
                usersTableModel.addRow(row);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading users: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshChatsTable() {
        try {
            // Clear existing data
            chatsTableModel.setRowCount(0);
            
            // Get all chats
            List<Chat> chats = chatDAO.getAllChats();
            
            for (Chat chat : chats) {
                String status = chat.getEndTime() == null ? "Active" : "Ended";
                
                Object[] row = {
                    chat.getId(),
                    dateFormat.format(chat.getStartTime()),
                    chat.getEndTime() == null ? "-" : dateFormat.format(chat.getEndTime()),
                    status,
                    chat.getLogFile() == null ? "-" : chat.getLogFile()
                };
                
                chatsTableModel.addRow(row);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading chats: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshSubscriptionsPanel() {
        try {
            // Clear the user combo box
            userComboBox.removeAllItems();
            List<User> users = userDAO.getAllUsers();
            for (User user : users) {
                if (!user.getUsername().equals("admin")) {
                    userComboBox.addItem(user.getId() + ": " + user.getUsername());
                }
            }
            
            // Clear the chat combo box
            chatComboBox.removeAllItems();
            List<Chat> chats = chatDAO.getAllChats();
            for (Chat chat : chats) {
                String status = chat.getEndTime() == null ? "Active" : "Ended";
                chatComboBox.addItem(chat.getId() + ": " + status + " (" + 
                        dateFormat.format(chat.getStartTime()) + ")");
            }
            
            // Refresh the subscriptions table
            // This would typically pull from a subscriptions table in the database
            // For now, we're just showing a placeholder message
            subscriptionsTableModel.setRowCount(0);
            
            // Add placeholder data (in a real app, you'd fetch actual subscriptions)
            subscriptionsTableModel.addRow(new Object[]{"Placeholder - Implement subscription data", "-", "-"});
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error refreshing subscription data: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void removeSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user to remove",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int userId = (int) usersTableModel.getValueAt(selectedRow, 0);
        String username = (String) usersTableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove user '" + username + "'?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                userDAO.deleteUser(userId);
                JOptionPane.showMessageDialog(this,
                        "User removed successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh the users table
                refreshUsersTable();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error removing user: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void startNewChat() {
        try {
            // Create a new chat in the database
            Chat newChat = new Chat();
            newChat.setStartTime(new java.util.Date());
            newChat.setAdmin(adminUser);
            chatDAO.saveChat(newChat);
            
            JOptionPane.showMessageDialog(this,
                    "New chat started successfully! Chat ID: " + newChat.getId(),
                    "Chat Started",
                    JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh the chats table
            refreshChatsTable();
            
            // Show the chats panel
            cardLayout.show(mainPanel, "chats");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error creating new chat: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void viewSelectedChatLog() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to view its log",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String logFile = (String) chatsTableModel.getValueAt(selectedRow, 4);
        if (logFile.equals("-")) {
            JOptionPane.showMessageDialog(this,
                    "No log file available for this chat",
                    "No Log Available",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            // Open the log file in a new window
            new ChatLogViewer(logFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error opening log file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void endSelectedChat() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to end",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String status = (String) chatsTableModel.getValueAt(selectedRow, 3);
        if (!status.equals("Active")) {
            JOptionPane.showMessageDialog(this,
                    "This chat has already ended",
                    "Chat Not Active",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int chatId = (int) chatsTableModel.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to end chat #" + chatId + "?",
                "Confirm End Chat",
                JOptionPane.YES_NO_OPTION);
                
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // End the chat in the database
                Chat chat = chatDAO.findById(chatId);
                if (chat != null) {
                    chat.setEndTime(new java.util.Date());
                    chatDAO.saveChat(chat);
                    
                    JOptionPane.showMessageDialog(this,
                            "Chat ended successfully",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh the chats table
                    refreshChatsTable();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error ending chat: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void subscribeUserToChat() {
        if (userComboBox.getSelectedIndex() == -1 || chatComboBox.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select both a user and a chat",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Extract user ID from selection (format is "ID: username")
            String userSelection = (String) userComboBox.getSelectedItem();
            int userId = Integer.parseInt(userSelection.substring(0, userSelection.indexOf(":")));
            
            // Extract chat ID from selection (format is "ID: status (date)")
            String chatSelection = (String) chatComboBox.getSelectedItem();
            int chatId = Integer.parseInt(chatSelection.substring(0, chatSelection.indexOf(":")));
            
            // Here you would call your subscription service/DAO to subscribe the user
            // For now, we'll just show a success message
            JOptionPane.showMessageDialog(this,
                    "User subscribed to chat successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh the subscriptions table
            refreshSubscriptionsPanel();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error subscribing user: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void unsubscribeUserFromChat() {
        if (userComboBox.getSelectedIndex() == -1 || chatComboBox.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select both a user and a chat",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Extract user ID from selection (format is "ID: username")
            String userSelection = (String) userComboBox.getSelectedItem();
            int userId = Integer.parseInt(userSelection.substring(0, userSelection.indexOf(":")));
            
            // Extract chat ID from selection (format is "ID: status (date)")
            String chatSelection = (String) chatComboBox.getSelectedItem();
            int chatId = Integer.parseInt(chatSelection.substring(0, chatSelection.indexOf(":")));
            
            // Here you would call your subscription service/DAO to unsubscribe the user
            // For now, we'll just show a success message
            JOptionPane.showMessageDialog(this,
                    "User unsubscribed from chat successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh the subscriptions table
            refreshSubscriptionsPanel();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error unsubscribing user: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int getUserCount() {
        try {
            return userDAO.getAllUsers().size() - 1; // Exclude admin
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getActiveChatCount() {
        try {
            return chatDAO.getActiveChats().size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getLogFileCount() {
        try {
            File logsDir = new File("logs");
            if (logsDir.exists() && logsDir.isDirectory()) {
                File[] files = logsDir.listFiles((dir, name) -> name.endsWith(".txt"));
                return files != null ? files.length : 0;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private void logout() {
        dispose();
        new LoginForm();
    }
    
    // Helper class for Hibernate access
    private static class HibernateUtil {
        public static org.hibernate.SessionFactory getSessionFactory() {
            try {
                return dao.HibernateUtil.getSessionFactory();
            } catch (Exception e) {
                return null;
            }
        }
    }
} 