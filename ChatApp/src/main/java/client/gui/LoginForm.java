package client.gui;

import client.services.ConnectionManager;
import dao.UserDAO;
import model.User;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatButtonBorder;
import com.formdev.flatlaf.ui.FlatRoundBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private final UserDAO userDAO;
    
    
    private final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private final Color BACKGROUND_COLOR = new Color(245, 246, 248);
    private final Color TEXT_COLOR = new Color(33, 33, 33);
    private final Color BUTTON_TEXT_COLOR = Color.WHITE;
    
    public LoginForm() {
        this.userDAO = new UserDAO();
        
        
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arrowType", "chevron");
            UIManager.put("Component.innerFocusWidth", 1);
            UIManager.put("Button.innerFocusWidth", 1);
            UIManager.put("TextField.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLightLaf");
        }
        
        
        setTitle("Chat Application");
        setSize(400, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        initComponents();
        layoutComponents();
        
        setVisible(true);
    }
    
    private void initComponents() {
        
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setMargin(new Insets(8, 10, 8, 10));
        
        
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setMargin(new Insets(8, 10, 8, 10));
        
        
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(BUTTON_TEXT_COLOR);
        loginButton.setBackground(PRIMARY_COLOR);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(120, 40));
        loginButton.addActionListener(e -> attemptLogin());
        
        
        registerButton = new JButton("Create New Account");
        registerButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        registerButton.setForeground(PRIMARY_COLOR);
        registerButton.setBackground(BACKGROUND_COLOR);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(true);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setPreferredSize(new Dimension(180, 40));
        registerButton.addActionListener(e -> {
            dispose();
            new RegisterForm();
        });
    }
    
    private void layoutComponents() {
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(30, 40, 15, 40));
        
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(BACKGROUND_COLOR);

        
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/images/chatapp.png"));
        Image scaledLogo = logoIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        headerPanel.add(logoLabel);

        JLabel titleLabel = new JLabel("Chat App");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        headerPanel.add(titleLabel);


        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        
        
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(TEXT_COLOR);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        formPanel.add(usernameField, gbc);
        
        
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(TEXT_COLOR);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(16, 0, 8, 0);
        formPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 8, 0);
        formPanel.add(passwordField, gbc);
        
        
        JPanel loginButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButtonPanel.setBackground(BACKGROUND_COLOR);
        loginButtonPanel.setBorder(new EmptyBorder(20, 0, 10, 0));
        loginButtonPanel.add(loginButton);
        
        
        JPanel registerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerButtonPanel.setBackground(BACKGROUND_COLOR);
        registerButtonPanel.setBorder(new EmptyBorder(5, 0, 20, 0));
        registerButtonPanel.add(registerButton);
        
        
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(BACKGROUND_COLOR);
        
        JLabel developerLabel = new JLabel("Developed by ZeroX");
        developerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        developerLabel.setForeground(new Color(120, 120, 120));
        footerPanel.add(developerLabel);
        
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BACKGROUND_COLOR);
        
        contentPanel.add(headerPanel);
        contentPanel.add(formPanel);
        contentPanel.add(loginButtonPanel);
        contentPanel.add(registerButtonPanel);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        add(mainPanel);
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
                
                dispose();
                
                if (username.equals("admin")) {
                    
                    new AdminDashboard(user);
                } else {
                    
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
