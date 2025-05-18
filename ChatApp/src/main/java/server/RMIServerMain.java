package server;

import dao.ChatDAO;
import dao.UserDAO;
import dao.HibernateUtil;
import server.observer.ChatSubscriptionManager;
import server.rmi.ChatServer;
import server.rmi.ChatServerImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.File;
import javax.swing.JOptionPane;

public class RMIServerMain {
    public static void main(String[] args) {
        try {
            System.out.println("Initializing Hibernate...");
            HibernateUtil.getSessionFactory(); 
            
            System.out.println("Creating logs directory...");
            createLogsDirectory();
            
            
            UserDAO userDAO = new UserDAO();
            ChatDAO chatDAO = new ChatDAO();
            
            
            ChatSubscriptionManager subscriptionManager = new ChatSubscriptionManager();
            
            
            System.out.println("Starting RMI Registry on port 1099...");
            Registry registry = LocateRegistry.createRegistry(1099);
            
            
            System.out.println("Initializing Chat Server...");
            ChatServer server = new ChatServerImpl(chatDAO, userDAO, subscriptionManager);
            registry.rebind("ChatService", server);
            
            
            createAdminIfNotExists(userDAO);
            
            System.out.println("Chat Server is running successfully!");
            System.out.println("Waiting for client connections...");
            
            
            createServerUI();
            
        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Failed to start the server: " + e.getMessage(), 
                "Server Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private static void createLogsDirectory() {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            if (logsDir.mkdirs()) {
                System.out.println("Logs directory created successfully");
            } else {
                System.err.println("Failed to create logs directory");
            }
        }
    }
    
    private static void createAdminIfNotExists(UserDAO userDAO) {
        if (userDAO.findByUsername("admin") == null) {
            System.out.println("Creating default admin account...");
            try {
                userDAO.createAdmin("admin@system.com", "admin", "password123", "Administrator", null);
                System.out.println("Admin account created successfully");
            } catch (Exception e) {
                System.err.println("Failed to create admin account: " + e.getMessage());
            }
        }
    }
    
    private static void createServerUI() {
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            ServerControlPanel controlPanel = new ServerControlPanel();
            controlPanel.setVisible(true);
        });
    }
}
