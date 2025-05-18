package client.gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UserProfilePanel extends JPanel {
    private final User user;
    private final UserDAO userDAO;
    
    private JTextField usernameField;
    private JTextField nicknameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel profilePicLabel;
    private String profilePicPath;
    
    public UserProfilePanel(User user, UserDAO userDAO) {
        this.user = user;
        this.userDAO = userDAO;
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        initComponents(formPanel);
        
        add(new JLabel("User Profile", JLabel.CENTER), BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> saveProfile());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void initComponents(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        
        profilePicPath = user.getProfilePic();
        profilePicLabel = new JLabel();
        profilePicLabel.setPreferredSize(new Dimension(100, 100));
        profilePicLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        if (profilePicPath != null && !profilePicPath.isEmpty()) {
            ImageIcon icon = new ImageIcon(profilePicPath);
            Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            profilePicLabel.setIcon(new ImageIcon(img));
        } else {
            profilePicLabel.setText("No Image");
            profilePicLabel.setHorizontalAlignment(JLabel.CENTER);
        }
        
        JButton chooseImageButton = new JButton("Choose Image");
        chooseImageButton.addActionListener(e -> chooseProfilePicture());
        
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(user.getUsername(), 20);
        
        
        JLabel nicknameLabel = new JLabel("Nickname:");
        nicknameField = new JTextField(user.getNickname(), 20);
        
        
        JLabel passwordLabel = new JLabel("New Password:");
        passwordField = new JPasswordField(20);
        
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmPasswordField = new JPasswordField(20);
        
        
        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(user.getEmail(), 20);
        emailField.setEditable(false);
        
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(profilePicLabel, gbc);
        
        gbc.gridy = 1;
        panel.add(chooseImageButton, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        panel.add(emailLabel, gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(usernameLabel, gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(nicknameLabel, gbc);
        gbc.gridx = 1;
        panel.add(nicknameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(confirmLabel, gbc);
        gbc.gridx = 1;
        panel.add(confirmPasswordField, gbc);
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
                
                
                ImageIcon icon = new ImageIcon(profilePicPath);
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                profilePicLabel.setIcon(new ImageIcon(img));
                profilePicLabel.setText("");
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                        "Error saving profile picture: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveProfile() {
        
        String username = usernameField.getText().trim();
        String nickname = nicknameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nickname cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        
        if (!password.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        
        user.setUsername(username);
        user.setNickname(nickname);
        if (!password.isEmpty()) {
            user.setPassword(password); 
        }
        if (profilePicPath != null) {
            user.setProfilePic(profilePicPath);
        }
        
        
        try {
            userDAO.updateUser(user);
            JOptionPane.showMessageDialog(this, "Profile updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                    "Error updating profile: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
} 