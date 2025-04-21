package client;

import client.gui.LoginForm;
import client.gui.SplashScreen;
import client.services.ConnectionManager;

import javax.swing.*;
import java.awt.*;

public class ChatApp {
    public static void main(String[] args) {
        try {
            // Set look and feel to match the system
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Show splash screen
            SplashScreen splashScreen = new SplashScreen();
            splashScreen.setVisible(true);
            
            // Initialize connection to server in background
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        // Initialize connection manager (which will connect to the RMI server)
                        ConnectionManager.getInstance();
                        return null;
                    } catch (Exception e) {
                        throw e;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        // Close splash screen
                        splashScreen.dispose();
                        
                        // Get results (this will throw exception if there was one)
                        get();
                        
                        // Start login screen
                        new LoginForm();
                    } catch (Exception e) {
                        splashScreen.dispose();
                        JOptionPane.showMessageDialog(null,
                                "Failed to connect to server: " + e.getMessage() + 
                                "\nPlease ensure the server is running.",
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                }
            };
            
            worker.execute();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error starting application: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
