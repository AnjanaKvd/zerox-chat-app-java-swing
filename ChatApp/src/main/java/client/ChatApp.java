package client;

import client.gui.LoginForm;
import client.gui.SplashScreen;
import client.services.ConnectionManager;

import javax.swing.*;
import java.awt.*;

public class ChatApp {
    public static void main(String[] args) {
        try {
            
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            
            SplashScreen splashScreen = new SplashScreen();
            splashScreen.setVisible(true);
            
            
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        
                        ConnectionManager.getInstance();
                        return null;
                    } catch (Exception e) {
                        throw e;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        
                        splashScreen.dispose();
                        
                        
                        get();
                        
                        
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
