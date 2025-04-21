package client.gui;

import client.services.ConnectionManager;
import dao.UserDAO;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private final UserDAO userDAO;
    
    public LoginForm() {
        this.userDAO = new UserDAO();
        
        // Setup window
        setTitle("Chat Application - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        layoutComponents();
        
        setVisible(true);
    }
    
    private void initComponents() {
        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(20);
        
        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);
        
        // Login button
        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> attemptLogin());
        
        // Register button
        registerButton = new JButton("Register");
        registerButton.addActionListener(e -> {
            dispose();
            new RegisterForm();
        });
    }
    
    private void layoutComponents() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Title
        JLabel titleLabel = new JLabel("Multi-User Chat Application");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gbc);
        
        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);
        
        // Add panel to frame
        add(panel);
    }
    
    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username and password cannot be empty",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            User user = userDAO.authenticateUser(username, password);
            
            if (user != null) {
                // Successful login
                dispose();
                
                if (username.equals("admin")) {
                    // Admin login
                    new AdminDashboard(user);
                } else {
                    // Regular user login
                    new UserDashboard(user);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Login error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
