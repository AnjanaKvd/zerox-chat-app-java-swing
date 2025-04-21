package client.gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import java.awt.*;

public class RegisterForm extends JFrame {
    private JTextField emailField, usernameField, nicknameField;
    private JPasswordField passwordField;
    private JButton registerBtn;
    private JButton loginBtn;

    public RegisterForm() {
        setTitle("Register");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        
        // Use a more flexible layout
        setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        JLabel titleLabel = new JLabel("User Registration", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        
        // Email field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        emailField = new JTextField(20);
        formPanel.add(emailField, gbc);
        
        // Username field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);
        
        // Password field
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);
        
        // Nickname field
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Nickname:"), gbc);
        
        gbc.gridx = 1;
        nicknameField = new JTextField(20);
        formPanel.add(nicknameField, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        registerBtn = new JButton("Register");
        loginBtn = new JButton("Back to Login");
        
        buttonPanel.add(registerBtn);
        buttonPanel.add(loginBtn);
        
        // Add action listeners
        registerBtn.addActionListener(e -> handleRegister());
        loginBtn.addActionListener(e -> {
            dispose();
            new LoginForm();
        });
        
        // Add panels to frame
        add(titleLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void handleRegister() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String nickname = nicknameField.getText().trim();

        // Validate fields
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email, Username, and Password are required!", 
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address", 
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", 
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Use nickname same as username if not provided
        if (nickname.isEmpty()) {
            nickname = username;
        }

        try {
            UserDAO dao = new UserDAO();
            
            // Check if username already exists
            if (dao.findByUsername(username) != null) {
                JOptionPane.showMessageDialog(this, "Username already exists!", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if email already exists
            if (dao.findByEmail(email) != null) {
                JOptionPane.showMessageDialog(this, "Email already exists!", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create the user using the createUser method in UserDAO
            dao.createUser(email, username, password, nickname, null);
            
            JOptionPane.showMessageDialog(this, "Registration successful!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginForm(); // Redirect to login
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
