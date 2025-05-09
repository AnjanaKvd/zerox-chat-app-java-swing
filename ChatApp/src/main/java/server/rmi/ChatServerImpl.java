package server.rmi;

import dao.ChatDAO;
import dao.UserDAO;
import model.Chat;
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
    
    // Add these fields to track chats by ID
    private Map<Integer, List<ChatClient>> chatRooms = new HashMap<>();
    private Map<ChatClient, Integer> clientChatMap = new HashMap<>();
    
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
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
        String joinMessage = nickname + " has joined : " + getCurrentTime();
        broadcastMessage(joinMessage);
        
        // Update user list for all clients
        updateAllClientUserLists();
    }
    
    @Override
    public void sendMessage(String message, String nickname) throws RemoteException {
        // Find the chat this client is in
        Integer chatId = null;
        ChatClient senderClient = null;
        
        for (Map.Entry<ChatClient, String> entry : connectedClients.entrySet()) {
            if (entry.getValue().equals(nickname)) {
                senderClient = entry.getKey();
                chatId = clientChatMap.get(senderClient);
                break;
            }
        }
        
        if (chatId != null) {
            String formattedMessage = nickname + ": " + message;
            
            // Log the message first to ensure it's saved
            Chat chat = chatDAO.findById(chatId);
            if (chat != null) {
                logMessageToChat(formattedMessage, chat);
            }
            
            // Then broadcast to all clients in the chat
            broadcastMessageToChat(formattedMessage, chatId);
            
            // Handle "Bye" command separately
            if (message.equalsIgnoreCase("Bye") && senderClient != null) {
                // The client will call removeClient explicitly
            }
        }
    }
    
    @Override
    public void removeClient(ChatClient client, String nickname) throws RemoteException {
        Integer chatId = clientChatMap.get(client);
        if (chatId != null) {
            // Notify remaining clients that a user left
            String leaveMessage = nickname + " left : " + getCurrentTime();
            broadcastMessageToChat(leaveMessage, chatId);
            
            // Remove the client from the chat room
            removeClientFromChat(client, chatId);
            
            // Update user list for remaining clients
            updateChatUserList(chatId);
            
            // Log the leave message
            Chat chat = chatDAO.findById(chatId);
            if (chat != null) {
                logMessageToChat(leaveMessage, chat);
            }
        } else {
            // If chatId is null, just remove from connected clients
            connectedClients.remove(client);
        }
    }
    
    @Override
    public void registerClientToChat(ChatClient client, String nickname, int chatId) throws RemoteException {
        // Add client to the connected clients
        connectedClients.put(client, nickname);
        
        // Add client to the specific chat room
        chatRooms.computeIfAbsent(chatId, k -> new ArrayList<>()).add(client);
        clientChatMap.put(client, chatId);
        
        // Look up chat from chatDAO
        Chat chat = chatDAO.findById(chatId);
        if (chat != null) {
            // Join message
            String joinMessage = nickname + " has joined : " + getCurrentTime();
            
            // Log the join message to the chat's log file
            logMessageToChat(joinMessage, chat);
            
            // Notify everyone in this chat that a new user joined
            broadcastMessageToChat(joinMessage, chatId);
            
            // Update user list for all clients in this chat
            updateChatUserList(chatId);
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
        chatLog.add("Chat started at: " + chatStartTime);
        
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

        } catch (IOException e) {
            System.err.println("Error saving or uploading chat log: " + e.getMessage());
        }
    }

    private String getCurrentTime() {
        return sdf.format(new Date());
    }

    // Helper method to broadcast message to specific chat
    private void broadcastMessageToChat(String message, int chatId) {
        List<ChatClient> clients = chatRooms.get(chatId);
        if (clients != null) {
            for (ChatClient client : clients) {
                try {
                    client.receiveMessage(message);
                } catch (RemoteException e) {
                    // If we can't reach the client, remove them
                    removeClientFromChat(client, chatId);
                }
            }
        }
    }

    // Helper method to update user list for a specific chat
    private void updateChatUserList(int chatId) {
        List<ChatClient> clients = chatRooms.get(chatId);
        if (clients != null && !clients.isEmpty()) {
            List<String> userList = new ArrayList<>();
            for (ChatClient client : clients) {
                String nickname = connectedClients.get(client);
                if (nickname != null) {
                    userList.add(nickname);
                }
            }
            
            String[] usersArray = userList.toArray(new String[0]);
            for (ChatClient client : clients) {
                try {
                    client.updateUserList(usersArray);
                } catch (RemoteException e) {
                    // If we can't reach the client, remove them
                    removeClientFromChat(client, chatId);
                }
            }
        }
    }

    // Helper method to remove client from a specific chat
    private void removeClientFromChat(ChatClient client, int chatId) {
        List<ChatClient> clients = chatRooms.get(chatId);
        if (clients != null) {
            clients.remove(client);
            if (clients.isEmpty()) {
                chatRooms.remove(chatId);
            }
        }
        clientChatMap.remove(client);
        connectedClients.remove(client);
    }

    // Helper method to log messages to chat file
    private synchronized void logMessageToChat(String message, Chat chat) {
        // Create logs directory if it doesn't exist
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
        
        String logFile = chat.getLogFile();
        if (logFile == null || logFile.isEmpty()) {
            // This should not happen as we create the log file during chat creation
            // But just in case, create one now
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String chatName = chat.getName() != null ? chat.getName() : "Chat_" + chat.getId();
            logFile = "logs/chat_" + chatName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".txt";
            
            // Update chat record
            chat.setLogFile(logFile);
            chatDAO.saveChat(chat);
            
            // Initialize the log file with a header
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                writer.write("Chat '" + chatName + "' created at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.newLine();
                writer.write("Created by admin: " + (chat.getAdmin() != null ? chat.getAdmin().getUsername() : "system"));
                writer.newLine();
                writer.write("-------------------------------------------");
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error creating chat log file: " + e.getMessage());
                return; // Exit if we couldn't create the file
            }
        }
        
        // Append to log file with synchronization
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to chat log: " + e.getMessage());
        }
    }
}
