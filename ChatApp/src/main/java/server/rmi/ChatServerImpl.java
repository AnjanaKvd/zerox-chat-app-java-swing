package server.rmi;

import dao.ChatDAO;
import dao.UserDAO;
import model.Chat;
import server.FTPUploader;
import server.observer.ChatClient;
import server.observer.ChatSubscriptionManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServerImpl extends UnicastRemoteObject implements ChatServer {
    private final Map<ChatClient, String> connectedClients;
    private boolean chatActive;
    private final SimpleDateFormat sdf;
    private String chatStartTime;
    private Chat currentChat;
    private final List<String> chatLog;
    private final ChatDAO chatDAO;
    private final UserDAO userDAO;
    private final ChatSubscriptionManager subscriptionManager;
    
    public ChatServerImpl(ChatDAO chatDAO) throws RemoteException {
        super();
        this.chatDAO = chatDAO;
        this.userDAO = null; // Not used in this constructor
        this.subscriptionManager = null; // Not used in this constructor
        connectedClients = new ConcurrentHashMap<>();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        chatLog = new ArrayList<>();
        chatActive = false;
    }
    
    public ChatServerImpl(ChatDAO chatDAO, UserDAO userDAO, ChatSubscriptionManager subscriptionManager) throws RemoteException {
        super();
        this.chatDAO = chatDAO;
        this.userDAO = userDAO;
        this.subscriptionManager = subscriptionManager;
        connectedClients = new ConcurrentHashMap<>();
        sdf = new SimpleDateFormat("yyyy-MM-dd : hh:mm a");
        chatLog = new ArrayList<>();
        chatActive = false;
    }
    
    @Override
    public void registerClient(ChatClient client, String nickname) throws RemoteException {
        connectedClients.put(client, nickname);
        
        if (!chatActive) {
            startChat();
        }
        
        // Notify all clients about the new user
        String joinMessage = nickname + " has joined  " + getCurrentTime();
        broadcastMessage(joinMessage);
        
        // Update user list for all clients
        updateAllClientUserLists();
    }
    
    @Override
    public void sendMessage(String message, String nickname) throws RemoteException {
        String formattedMessage = nickname + ": " + message;
        broadcastMessage(formattedMessage);
        chatLog.add(formattedMessage);
        
        // Handle "Bye" command separately to remove the client
        if (message.equalsIgnoreCase("Bye")) {
            // The client will call removeClient explicitly
        }
    }
    
    @Override
    public void removeClient(ChatClient client, String nickname) throws RemoteException {
        connectedClients.remove(client);
        
        if (connectedClients.isEmpty()) {
            endChat();
        } else {
            // Notify remaining clients that a user left
            String leaveMessage = nickname + " left " + getCurrentTime();
            broadcastMessage(leaveMessage);
            
            // Update user list for remaining clients
            updateAllClientUserLists();
        }
    }
    
    private void broadcastMessage(String message) {
        for (Map.Entry<ChatClient, String> entry : connectedClients.entrySet()) {
            try {
                entry.getKey().receiveMessage(message);
            } catch (RemoteException e) {
                // If we can't reach the client, remove them
                connectedClients.remove(entry.getKey());
            }
        }
    }
    
    private void updateAllClientUserLists() {
        String[] userList = connectedClients.values().toArray(new String[0]);
        
        for (ChatClient client : connectedClients.keySet()) {
            try {
                client.updateUserList(userList);
            } catch (RemoteException e) {
                // If we can't reach the client, remove them
                connectedClients.remove(client);
            }
        }
    }
    
    private void startChat() {
        chatActive = true;
        chatStartTime = getCurrentTime();
        
        // Create a new chat record
        currentChat = new Chat();
        currentChat.setStartTime(new Date());
        chatDAO.saveChat(currentChat);
        
        // Log start time
        chatLog.add("Chat started at " + chatStartTime);

        
        // Notify all connected clients
        for (ChatClient client : connectedClients.keySet()) {
            try {
                client.notifyChatStarted(chatStartTime);
            } catch (RemoteException e) {
                connectedClients.remove(client);
            }
        }
    }
    
    private void endChat() {
        chatActive = false;
        String endTime = getCurrentTime();
        
        // Log end time
        chatLog.add("Chat ended at: " + endTime);
        
        // Save chat to file
        String logFileName = "chat_" + chatStartTime.replace(":", "-").replace(" ", "_") + ".txt";
        saveLogToFile(logFileName);
        
        // Update chat record in database
        if (currentChat != null) {
            currentChat.setEndTime(new Date());
            currentChat.setLogFile(logFileName);
            chatDAO.saveChat(currentChat);
        }
        
        // Clear chat log
        chatLog.clear();
    }

    private void saveLogToFile(String fileName) {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            File logFile = new File(logsDir, fileName);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                for (String line : chatLog) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            System.out.println("✅ Log saved locally: " + logFile.getAbsolutePath());

            // Upload to FTP
            FTPUploader uploader = new FTPUploader("eu-central-1.sftpcloud.io", 21, "009fc42fab6d456fab102d4c86c991df", "FkgDvxNJHs6LmAYXD484HuxSkT2l8cJE");
            boolean uploaded = uploader.uploadFile(
                    logFile.getAbsolutePath(),
                    "/chatlogs",                // Remote directory
                    fileName                    // Remote file name
            );

            if (uploaded) {
                System.out.println("✅ Log file uploaded to FTP.");
            } else {
                System.out.println("❌ Failed to upload log to FTP.");
            }

        } catch (IOException e) {
            System.err.println("Error saving or uploading chat log: " + e.getMessage());
        }
    }


    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        Date now = new Date();
        return ":"+ sdf.format(now).toLowerCase() ;
    }

//    private String getCurrentTime() {
//        return sdf.format(new Date());
//    }
}
