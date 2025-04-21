package client.services;

import server.rmi.ChatServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ConnectionManager {
    private static final String SERVER_URL = "rmi://localhost:1099/ChatService";
    private static ConnectionManager instance;
    private ChatServer chatServer;
    private ChatClientImpl chatClient;
    
    private ConnectionManager() {
        try {
            System.out.println("Connecting to RMI server at: " + SERVER_URL);
            chatServer = (ChatServer) Naming.lookup(SERVER_URL);
            System.out.println("Connected to server successfully");
            
            try {
                chatClient = new ChatClientImpl();
                System.out.println("Chat client created successfully");
            } catch (RemoteException e) {
                System.err.println("Failed to create chat client: " + e.getMessage());
                chatClient = null;
                throw e;
            }
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            System.err.println("Failed to connect to RMI server: " + e.getMessage());
            e.printStackTrace();
            // Still allow the app to start, but with limited functionality
            chatServer = null;
        }
    }
    
    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }
    
    public ChatServer getChatServer() {
        return chatServer;
    }
    
    public ChatClientImpl getChatClient() {
        return chatClient;
    }
    
    public boolean isConnected() {
        return chatServer != null && chatClient != null;
    }
    
    public String getConnectionStatus() {
        if (chatServer == null) {
            return "Not connected to server";
        } else if (chatClient == null) {
            return "Server connected, but client not initialized";
        } else {
            return "Connected";
        }
    }
} 