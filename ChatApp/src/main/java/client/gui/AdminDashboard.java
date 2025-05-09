package client.gui;

import client.services.ConnectionManager;
import dao.ChatDAO;
import dao.UserDAO;
import model.Chat;
import model.User;
import server.rmi.ChatServer;
import org.hibernate.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;

public class AdminDashboard extends JFrame {
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

    public AdminDashboard(User admin) {
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
        JPanel navPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton homeButton = new JButton("Dashboard Home");
        JButton usersButton = new JButton("Manage Users");
        JButton chatsButton = new JButton("Manage Chats");
        JButton subscriptionsButton = new JButton("Manage Subscriptions");
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
        logoutButton.addActionListener(e -> logout());

        navPanel.add(homeButton);
        navPanel.add(usersButton);
        navPanel.add(chatsButton);
        navPanel.add(subscriptionsButton);
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
        String[] columnNames = {"ID", "Name", "Start Time", "End Time", "Status", "Log File"};
        chatsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };

        chatsTable = new JTable(chatsTableModel);
        chatsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(chatsTable);

        // Top control panel for creating new chats
        JPanel createChatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        createChatPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel chatNameLabel = new JLabel("Chat Name:");
        JTextField chatNameField = new JTextField(20);
        JButton createChatButton = new JButton("Create New Chat");
        
        createChatButton.addActionListener(e -> {
            String chatName = chatNameField.getText().trim();
            if (chatName.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a name for the chat", 
                    "Missing Chat Name", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            createNewChat(chatName);
            chatNameField.setText(""); // Clear the field after creation
        });
        
        createChatPanel.add(chatNameLabel);
        createChatPanel.add(chatNameField);
        createChatPanel.add(createChatButton);

        // Action buttons for managing existing chats
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Chats");
        JButton viewLogButton = new JButton("View Selected Log");
        JButton endChatButton = new JButton("End Selected Chat");
        JButton deleteChatButton = new JButton("Delete Selected Chat");

        refreshButton.addActionListener(e -> refreshChatsTable());
        viewLogButton.addActionListener(e -> viewSelectedChatLog());
        endChatButton.addActionListener(e -> endSelectedChat());
        deleteChatButton.addActionListener(e -> deleteSelectedChat());
        
        // Style the delete button to indicate danger
        deleteChatButton.setBackground(new Color(211, 47, 47)); // Red
        deleteChatButton.setForeground(Color.WHITE);

        actionPanel.add(refreshButton);
        actionPanel.add(viewLogButton);
        actionPanel.add(endChatButton);
        actionPanel.add(deleteChatButton);

        // Main panel layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(createChatPanel, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);

        // Initial load of chats
        refreshChatsTable();

        return panel;
    }

    private JPanel createSubscriptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Subscription Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Create the top panel for adding subscriptions
        JPanel addSubscriptionPanel = new JPanel(new BorderLayout(10, 0));
        addSubscriptionPanel.setBorder(BorderFactory.createTitledBorder("Add New Subscription"));
        
        // User and chat selection
        JPanel selectionPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        
        JLabel userLabel = new JLabel("Select User:", JLabel.RIGHT);
        userComboBox = new JComboBox<>();
        
        JLabel chatLabel = new JLabel("Select Chat:", JLabel.RIGHT);
        chatComboBox = new JComboBox<>();
        
        JButton subscribeButton = new JButton("Subscribe User to Chat");
        subscribeButton.setBackground(new Color(46, 125, 50)); // Green
        subscribeButton.setForeground(Color.WHITE);
        
        selectionPanel.add(userLabel);
        selectionPanel.add(userComboBox);
        selectionPanel.add(new JLabel()); // Empty cell for grid alignment
        selectionPanel.add(chatLabel);
        selectionPanel.add(chatComboBox);
        selectionPanel.add(subscribeButton);
        
        addSubscriptionPanel.add(selectionPanel, BorderLayout.CENTER);
        
        // Subscribe button action
        subscribeButton.addActionListener(e -> subscribeUserToChat());
        
        // Create the table for existing subscriptions
        JPanel subscriptionsTablePanel = new JPanel(new BorderLayout(0, 10));
        subscriptionsTablePanel.setBorder(BorderFactory.createTitledBorder("Current Subscriptions"));
        
        String[] columnNames = {"User ID", "Username", "Chat ID", "Chat Name", "Subscribed Since"};
        subscriptionsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        subscriptionsTable = new JTable(subscriptionsTableModel);
        subscriptionsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(subscriptionsTable);
        
        // Action buttons for the table
        JPanel tableActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton refreshButton = new JButton("Refresh List");
        refreshButton.addActionListener(e -> refreshSubscriptionsPanel());
        
        JButton removeButton = new JButton("Remove Selected");
        removeButton.setBackground(new Color(211, 47, 47)); // Red
        removeButton.setForeground(Color.WHITE);
        removeButton.addActionListener(e -> removeSelectedSubscription());
        
        tableActionsPanel.add(refreshButton);
        tableActionsPanel.add(removeButton);
        
        subscriptionsTablePanel.add(tableScrollPane, BorderLayout.CENTER);
        subscriptionsTablePanel.add(tableActionsPanel, BorderLayout.SOUTH);
        
        // Put it all together
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 10));
        mainContentPanel.add(addSubscriptionPanel, BorderLayout.NORTH);
        mainContentPanel.add(subscriptionsTablePanel, BorderLayout.CENTER);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(mainContentPanel, BorderLayout.CENTER);
        
        // Initial load
        refreshSubscriptionsPanel();

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
            // Clear existing rows
            chatsTableModel.setRowCount(0);

            // Get all chats from the database
            List<Chat> chats = chatDAO.getAllChats();

            // Format for date display
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            // Add chats to the table
            for (Chat chat : chats) {
                String startTime = chat.getStartTime() != null ? sdf.format(chat.getStartTime()) : "-";
                String endTime = chat.getEndTime() != null ? sdf.format(chat.getEndTime()) : "-";
                String status = chat.getEndTime() == null ? "Active" : "Ended";
                String logFile = chat.getLogFile() != null ? chat.getLogFile() : "-";
                String name = chat.getName() != null ? chat.getName() : "Chat #" + chat.getId();

                chatsTableModel.addRow(new Object[]{
                        chat.getId(),
                    name,
                    startTime,
                    endTime,
                        status,
                    logFile
                });
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
            // Clear existing data
            userComboBox.removeAllItems();
            chatComboBox.removeAllItems();
            subscriptionsTableModel.setRowCount(0);
            
            // Load users
            List<User> users = userDAO.getAllUsers();
            for (User user : users) {
                userComboBox.addItem(user.getUsername() + " (ID: " + user.getId() + ")");
            }
            
            // Load chats (active ones)
            List<Chat> chats = chatDAO.getActiveChats();
            for (Chat chat : chats) {
                String chatName = chat.getName() != null ? chat.getName() : "Chat #" + chat.getId();
                chatComboBox.addItem(chatName + " (ID: " + chat.getId() + ")");
            }
            
            // Load subscriptions using a safer approach that doesn't rely on specific SQL dialect
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            for (User user : users) {
                // Get the chats this user is subscribed to
                List<Chat> userChats = chatDAO.getSubscribedChats(user.getId());
                
                for (Chat chat : userChats) {
                    String chatName = chat.getName() != null ? chat.getName() : "Chat #" + chat.getId();
                    String subscriptionDate = chat.getStartTime() != null ? 
                            dateFormat.format(chat.getStartTime()) : "N/A";
                    
                    subscriptionsTableModel.addRow(new Object[]{
                        user.getId(),
                        user.getUsername(),
                        chat.getId(),
                        chatName,
                        subscriptionDate
                    });
                }
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error refreshing subscriptions panel: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
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
        try {
            // Make sure there are selections
            if (userComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                        "Please select a user to subscribe.", 
                        "No User Selected", 
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (chatComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, 
                        "Please select a chat to subscribe to.", 
                        "No Chat Selected", 
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Extract IDs from selections
            String userSelection = userComboBox.getSelectedItem().toString();
            String chatSelection = chatComboBox.getSelectedItem().toString();
            
            int userId = extractIdFromSelection(userSelection);
            int chatId = extractIdFromSelection(chatSelection);
            
            // Get user and chat objects
            User user = userDAO.findById(userId);
            Chat chat = chatDAO.findById(chatId);
            
            if (user == null || chat == null) {
                JOptionPane.showMessageDialog(this, 
                        "Selected user or chat not found. Please refresh and try again.", 
                        "Not Found", 
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if already subscribed
            List<Integer> subscribedChatIds = chatDAO.getSubscribedChatIds(userId);
            if (subscribedChatIds.contains(chatId)) {
                JOptionPane.showMessageDialog(this, 
                        "User " + user.getUsername() + " is already subscribed to this chat.", 
                        "Already Subscribed", 
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Subscribe the user
            chatDAO.subscribeUserToChat(userId, chatId);
            
            JOptionPane.showMessageDialog(this, 
                    "Successfully subscribed " + user.getUsername() + " to " + 
                    (chat.getName() != null ? chat.getName() : "Chat #" + chat.getId()), 
                    "Subscription Added", 
                    JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh the panel
            refreshSubscriptionsPanel();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error subscribing user to chat: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void removeSelectedSubscription() {
        // Check if there's a selection
        int selectedRow = subscriptionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                    "Please select a subscription to remove.", 
                    "No Selection", 
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Get the IDs and names for confirmation
            int userId = (int) subscriptionsTableModel.getValueAt(selectedRow, 0);
            String username = (String) subscriptionsTableModel.getValueAt(selectedRow, 1);
            int chatId = (int) subscriptionsTableModel.getValueAt(selectedRow, 2);
            String chatName = (String) subscriptionsTableModel.getValueAt(selectedRow, 3);
            
            // Confirm with the admin
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to unsubscribe user '" + username + 
                    "' from chat '" + chatName + "'?",
                    "Confirm Unsubscription",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                // Do the unsubscription
                chatDAO.unsubscribeUserFromChat(userId, chatId);
                
                JOptionPane.showMessageDialog(this, 
                        "User '" + username + "' has been unsubscribed from '" + chatName + "'", 
                        "Subscription Removed", 
                        JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh the table
                refreshSubscriptionsPanel();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error removing subscription: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private int extractIdFromSelection(String selection) {
        try {
            int idStart = selection.lastIndexOf("(ID: ") + 5;
            int idEnd = selection.lastIndexOf(")");
            
            if (idStart > 5 && idEnd > idStart) {
                String idStr = selection.substring(idStart, idEnd);
                return Integer.parseInt(idStr);
            }
            
            // If ID not found in expected format, try to find any number
            for (int i = selection.length() - 1; i >= 0; i--) {
                if (Character.isDigit(selection.charAt(i))) {
                    // Find the start of this number
                    int numStart = i;
                    while (numStart >= 0 && Character.isDigit(selection.charAt(numStart))) {
                        numStart--;
                    }
                    numStart++; // Adjust after loop
                    
                    return Integer.parseInt(selection.substring(numStart, i + 1));
                }
            }
            
            throw new IllegalArgumentException("No ID found in: " + selection);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not extract ID from: " + selection);
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

    private void createNewChat(String chatName) {
        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }
            
            // Create a new chat in the database
            Chat newChat = new Chat();
            newChat.setStartTime(new java.util.Date());
            newChat.setAdmin(adminUser);
            newChat.setName(chatName);
            
            // Create a log file name immediately
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String logFileName = "logs/chat_" + chatName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".txt";
            newChat.setLogFile(logFileName);
            
            // Initialize the log file with a header
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName))) {
                writer.write("Chat '" + chatName + "' created at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.newLine();
                writer.write("Created by admin: " + adminUser.getUsername());
                writer.newLine();
                writer.write("-------------------------------------------");
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error creating chat log file: " + e.getMessage());
            }
            
            // Save the chat with its log file
            chatDAO.saveChat(newChat);

            JOptionPane.showMessageDialog(this,
                    "New chat created successfully! Chat ID: " + newChat.getId(),
                    "Chat Created",
                    JOptionPane.INFORMATION_MESSAGE);

            // Refresh the chats table
            refreshChatsTable();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error creating new chat: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedChat() {
        int selectedRow = chatsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a chat to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int chatId = (int) chatsTableModel.getValueAt(selectedRow, 0);
        String chatName = (String) chatsTableModel.getValueAt(selectedRow, 1);
        String status = (String) chatsTableModel.getValueAt(selectedRow, 4);
        
        // Confirm before deleting
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete chat '" + chatName + "'?\n" +
                "This will permanently delete all chat data and log files.\n" +
                (status.equals("Active") ? "WARNING: This chat is currently active!" : ""),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // End the chat first if it's active
                if (status.equals("Active")) {
                    String logFileName = "chat_" + chatId + "_" + 
                            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
                    chatDAO.endChat(chatId, logFileName);
                }
                
                // Delete the chat
                chatDAO.deleteChat(chatId);
                
                JOptionPane.showMessageDialog(this,
                        "Chat deleted successfully!",
                        "Chat Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh the table
                refreshChatsTable();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error deleting chat: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
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