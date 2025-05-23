package client.gui;

import dao.UserDAO;
import model.User;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class RegisterForm extends JFrame {
    private JTextField emailField, usernameField, nicknameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton registerBtn;
    private JButton loginBtn;
    private JLabel profilePicLabel;
    private String profilePicPath;
    private JLabel profileImageDisplay;
    
    
    private final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private final Color BACKGROUND_COLOR = new Color(245, 246, 248);
    private final Color TEXT_COLOR = new Color(33, 33, 33);
    private final Color BUTTON_TEXT_COLOR = Color.WHITE;

    public RegisterForm() {
        
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
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        initComponents();
        layoutComponents();
        
        setVisible(true);
    }
    
    private void initComponents() {
        
        emailField = new JTextField(20);
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailField.setMargin(new Insets(8, 10, 8, 10));
        
        
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setMargin(new Insets(8, 10, 8, 10));
        
        
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setMargin(new Insets(8, 10, 8, 10));
        
        
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        confirmPasswordField.setMargin(new Insets(8, 10, 8, 10));
        
        
        nicknameField = new JTextField(20);
        nicknameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nicknameField.setMargin(new Insets(8, 10, 8, 10));
        
        
        profilePicLabel = new JLabel("Upload Image", SwingConstants.LEFT);
        profilePicLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        profilePicLabel.setForeground(BUTTON_TEXT_COLOR);
        profilePicLabel.setOpaque(true);
        profilePicLabel.setBackground(PRIMARY_COLOR);
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profilePicLabel.setPreferredSize(new Dimension(140, 40));
        profilePicLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        profilePicLabel.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 0, true));
        
        
        ImageIcon uploadIcon = new ImageIcon(getClass().getResource("/images/upload_icon.png"));
        if (uploadIcon.getIconWidth() > 0) {
            Image scaledIcon = uploadIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            profilePicLabel.setIcon(new ImageIcon(scaledIcon));
        } else {
            
            profilePicLabel.setText("Upload Image");
        }
        
        profilePicLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseProfilePicture();
            }
        });
        
        
        profileImageDisplay = new JLabel();
        profileImageDisplay.setPreferredSize(new Dimension(50, 50));
        profileImageDisplay.setBorder(BorderFactory.createEmptyBorder());
        
        
        registerBtn = new JButton("Register");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerBtn.setForeground(BUTTON_TEXT_COLOR);
        registerBtn.setBackground(PRIMARY_COLOR);
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerBtn.setPreferredSize(new Dimension(120, 40));
        registerBtn.addActionListener(e -> handleRegister());
        
        
        loginBtn = new JButton("Back to Login");
        loginBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginBtn.setForeground(PRIMARY_COLOR);
        loginBtn.setBackground(BACKGROUND_COLOR);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(true);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.setPreferredSize(new Dimension(180, 40));
        loginBtn.addActionListener(e -> {
            dispose();
            new LoginForm();
        });
    }
    
    private void layoutComponents() {
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(25, 40, 15, 40));
        
        
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
        
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        
        
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailLabel.setForeground(TEXT_COLOR);
        
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(TEXT_COLOR);
        
        JLabel confirmPasswordLabel = new JLabel("Confirm Password");
        confirmPasswordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        confirmPasswordLabel.setForeground(TEXT_COLOR);
        
        
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(TEXT_COLOR);
        
        JLabel nicknameLabel = new JLabel("Nickname");
        nicknameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nicknameLabel.setForeground(TEXT_COLOR);
        
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        formPanel.add(emailLabel, gbc);
        
        gbc.gridy = 1;
        formPanel.add(emailField, gbc);
        
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        formPanel.add(usernameLabel, gbc);
        
        gbc.gridy = 1;
        formPanel.add(usernameField, gbc);
        
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(16, 8, 8, 8);
        formPanel.add(passwordLabel, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 8, 8, 8);
        formPanel.add(passwordField, gbc);
        
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.insets = new Insets(16, 8, 8, 8);
        formPanel.add(nicknameLabel, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 8, 8, 8);
        formPanel.add(nicknameField, gbc);
        
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.insets = new Insets(16, 8, 8, 8);
        formPanel.add(confirmPasswordLabel, gbc);
        
        gbc.gridy = 5;
        gbc.insets = new Insets(8, 8, 8, 8);
        formPanel.add(confirmPasswordField, gbc);
        
        
        JPanel profilePicPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        profilePicPanel.setBackground(BACKGROUND_COLOR);
        profilePicPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        profilePicPanel.add(profilePicLabel);
        profilePicPanel.add(profileImageDisplay);
        
        
        JPanel registerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerButtonPanel.setBackground(BACKGROUND_COLOR);
        registerButtonPanel.setBorder(new EmptyBorder(20, 0, 10, 0));
        registerButtonPanel.add(registerBtn);
        
        
        JPanel loginButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButtonPanel.setBackground(BACKGROUND_COLOR);
        loginButtonPanel.setBorder(new EmptyBorder(5, 0, 10, 0));
        loginButtonPanel.add(loginBtn);
        
        
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
        contentPanel.add(profilePicPanel);  
        contentPanel.add(registerButtonPanel);
        contentPanel.add(loginButtonPanel);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }
    
    private void chooseProfilePicture() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            try {
                
                File resourcesDir = new File("profile_pics");
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs();
                }
                
                
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                Path targetPath = Paths.get(resourcesDir.getAbsolutePath(), fileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                
                profilePicPath = targetPath.toString();
                
                
                displayCircularImage(profilePicPath);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                        "Error saving profile picture: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void displayCircularImage(String imagePath) {
        ImageIcon imageIcon = new ImageIcon(imagePath);
        if (imageIcon.getIconWidth() > 0) {
            
            Image image = imageIcon.getImage();
            int diameter = 50;
            BufferedImage bi = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) bi.createGraphics();
            
            
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Ellipse2D.Double shape = new Ellipse2D.Double(0, 0, diameter, diameter);
            g2.setClip(shape);
            
            
            g2.drawImage(image, 0, 0, diameter, diameter, null);
            g2.dispose();
            
            
            profileImageDisplay.setIcon(new ImageIcon(bi));
        }
    }

    private void handleRegister() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String nickname = nicknameField.getText().trim();

        
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
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", 
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        
        if (nickname.isEmpty()) {
            nickname = username;
        }

        try {
            UserDAO dao = new UserDAO();
            
            
            if (dao.findByUsername(username) != null) {
                JOptionPane.showMessageDialog(this, "Username already exists!", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            
            if (dao.findByEmail(email) != null) {
                JOptionPane.showMessageDialog(this, "Email already exists!", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            
            dao.createUser(email, username, password, nickname, profilePicPath);
            
            JOptionPane.showMessageDialog(this, "Registration successful!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginForm(); 
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
