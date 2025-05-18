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
    
    
    private Map<Integer, List<ChatClient>> chatRooms = new HashMap<>();
    private Map<ChatClient, Integer> clientChatMap = new HashMap<>();
    
    public ChatServerImpl(ChatDAO chatDAO) throws RemoteException {
        super();
        this.chatDAO = chatDAO;
        this.userDAO = null; 
        this.subscriptionManager = null; 
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
        // Check if the client is already registered to avoid duplicates
        if (connectedClients.containsKey(client) || isNicknameAlreadyConnected(nickname)) {
            throw new RemoteException("User with nickname " + nickname + " is already connected");
        }
        
        connectedClients.put(client, nickname);
        
        if (!chatActive) {
            startChat();
        }
        
        String joinMessage = nickname + " has joined : " + getCurrentTime();
        broadcastMessage(joinMessage);
        
        updateAllClientUserLists();
    }
    
    @Override
    public void sendMessage(String message, String nickname) throws RemoteException {
        // Find the client and their chat room
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
            
            try {
                // Log the message to the chat's log file
                Chat chat = chatDAO.findById(chatId);
                if (chat != null) {
                    logMessageToChat(formattedMessage, chat);
                } else {
                    System.err.println("Error: Could not find chat with ID " + chatId);
                    return;
                }
                
                // Send the message to all connected clients in this chat
                broadcastMessageToChat(formattedMessage, chatId);
                
                // Handle "Bye" command (handled in client)
            } catch (Exception e) {
                System.err.println("Error in sendMessage: " + e.getMessage());
                e.printStackTrace();
                throw new RemoteException("Error processing message: " + e.getMessage());
            }
        } else {
            System.err.println("Error: Could not determine chat ID for user " + nickname);
            throw new RemoteException("You are not connected to any chat room");
        }
    }
    
    @Override
    public void removeClient(ChatClient client, String nickname) throws RemoteException {
        Integer chatId = clientChatMap.get(client);
        if (chatId != null) {
            
            String leaveMessage = nickname + " left : " + getCurrentTime();
            broadcastMessageToChat(leaveMessage, chatId);
            
            
            removeClientFromChat(client, chatId);
            
            
            updateChatUserList(chatId);
            
            
            Chat chat = chatDAO.findById(chatId);
            if (chat != null) {
                logMessageToChat(leaveMessage, chat);
            }
        } else {
            
            connectedClients.remove(client);
        }
    }
    
    @Override
    public void registerClientToChat(ChatClient client, String nickname, int chatId) throws RemoteException {
        // Check if client is already registered to this chat
        if (clientChatMap.containsKey(client) && clientChatMap.get(client) == chatId) {
            return; // Client already registered to this chat
        }
        
        // Register the client if not already registered
        if (!connectedClients.containsKey(client)) {
            connectedClients.put(client, nickname);
        }
        
        // Register to chat room
        List<ChatClient> clients = chatRooms.computeIfAbsent(chatId, k -> new ArrayList<>());
        if (!clients.contains(client)) {
            clients.add(client);
        }
        clientChatMap.put(client, chatId);
        
        // Process chat
        Chat chat = chatDAO.findById(chatId);
        if (chat != null) {
            // Log join message
            String joinMessage = nickname + " has joined : " + getCurrentTime();
            
            // Update chat log
            logMessageToChat(joinMessage, chat);
            
            // Notify all clients in the chat
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
                
                connectedClients.remove(entry.getKey());
            }
        }
    }
    
    private void updateAllClientUserLists() {
        // Create a Set to avoid duplicate usernames
        Set<String> uniqueUsers = new HashSet<>(connectedClients.values());
        String[] userList = uniqueUsers.toArray(new String[0]);
        
        for (ChatClient client : connectedClients.keySet()) {
            try {
                client.updateUserList(userList);
            } catch (RemoteException e) {
                // Remove disconnected client
                connectedClients.remove(client);
            }
        }
    }
    
    private void startChat() {
        chatActive = true;
        chatStartTime = getCurrentTime();
        
        
        currentChat = new Chat();
        currentChat.setStartTime(new Date());
        chatDAO.saveChat(currentChat);
        
        
        chatLog.add("Chat started at: " + chatStartTime);
        
        
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
        
        
        chatLog.add("Chat ended at: " + endTime);
        
        
        String logFileName = "chat_" + chatStartTime.replace(":", "-").replace(" ", "_") + ".txt";
        saveLogToFile(logFileName);
        
        
        if (currentChat != null) {
            currentChat.setEndTime(new Date());
            currentChat.setLogFile(logFileName);
            chatDAO.saveChat(currentChat);
        }
        
        
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

    
    private void broadcastMessageToChat(String message, int chatId) {
        List<ChatClient> clients = chatRooms.get(chatId);
        if (clients != null) {
            for (ChatClient client : clients) {
                try {
                    client.receiveMessage(message);
                } catch (RemoteException e) {
                    
                    removeClientFromChat(client, chatId);
                }
            }
        }
    }

    
    private void updateChatUserList(int chatId) {
        List<ChatClient> clients = chatRooms.get(chatId);
        if (clients != null && !clients.isEmpty()) {
            // Use Set to avoid duplicates
            Set<String> uniqueUsers = new HashSet<>();
            for (ChatClient client : clients) {
                String nickname = connectedClients.get(client);
                if (nickname != null) {
                    uniqueUsers.add(nickname);
                }
            }
            
            String[] usersArray = uniqueUsers.toArray(new String[0]);
            for (ChatClient client : clients) {
                try {
                    client.updateUserList(usersArray);
                } catch (RemoteException e) {
                    // Remove disconnected client
                    removeClientFromChat(client, chatId);
                }
            }
        }
    }

    
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

    
    private synchronized void logMessageToChat(String message, Chat chat) {
        // Create logs directory if it doesn't exist
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
        
        String logFile = chat.getLogFile();
        if (logFile == null || logFile.isEmpty()) {
            // Generate a new log file name with timestamp and ensure it's unique
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String chatName = chat.getName() != null ? chat.getName() : "Chat_" + chat.getId();
            logFile = "logs/chat_" + chatName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".txt";
            
            // Update the chat with the new log file path
            chat.setLogFile(logFile);
            try {
                chatDAO.saveChat(chat);
            } catch (Exception e) {
                System.err.println("Error saving chat with log file path: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Initialize the log file with header information
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                writer.write("[HEADER]Chat '" + chatName + "' created at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.newLine();
                writer.write("[ADMIN]Created by admin: " + (chat.getAdmin() != null ? chat.getAdmin().getUsername() : "system"));
                writer.newLine();
                writer.write("[SEPARATOR]-------------------------------------------");
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error creating chat log file: " + e.getMessage());
                e.printStackTrace();
                return; // Exit if we can't create the log file
            }
        }
        
        // Prevent duplicate messages by checking if the last written message is identical
        String lastMessage = readLastMessageFromLog(logFile);
        if (lastMessage != null && lastMessage.equals(message)) {
            System.out.println("Duplicate message detected, skipping: " + message);
            return;
        }
        
        // Add identifier before writing message to log file
        String messageWithIdentifier = message;
        
        if (message.contains(" has joined : ")) {
            messageWithIdentifier = "[JOIN]" + message;
        } else if (message.contains(" left : ")) {
            messageWithIdentifier = "[LEAVE]" + message;
        } else if (message.contains(": ")) {
            messageWithIdentifier = "[MSG]" + message;
        } else {
            messageWithIdentifier = "[SYSTEM]" + message;
        }
        
        // Append the message to the log file with retries
        int maxRetries = 3;
        boolean success = false;
        
        for (int retry = 0; retry < maxRetries && !success; retry++) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(messageWithIdentifier);
                writer.newLine();
                writer.flush(); // Ensure content is written to disk
                success = true;
            } catch (IOException e) {
                System.err.println("Error writing to chat log (attempt " + (retry+1) + "): " + e.getMessage());
                e.printStackTrace();
                
                // Wait a moment before retrying
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (!success) {
            System.err.println("Failed to write message to log after " + maxRetries + " attempts: " + messageWithIdentifier);
        }
    }

    // Add helper method to read last message from log file
    private String readLastMessageFromLog(String logFile) {
        try {
            File file = new File(logFile);
            if (!file.exists() || file.length() == 0) {
                return null;
            }
            
            String lastLine = null;
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                long fileLength = file.length() - 1;
                StringBuilder sb = new StringBuilder();
                
                for(long pointer = fileLength; pointer >= 0; pointer--) {
                    raf.seek(pointer);
                    char c = (char)raf.read();
                    
                    if (c == '\n' && sb.length() > 0) {
                        lastLine = sb.reverse().toString();
                        break;
                    }
                    
                    if (c != '\r') {
                        sb.append(c);
                    }
                }
                
                if (lastLine == null && sb.length() > 0) {
                    lastLine = sb.reverse().toString();
                }
            }
            
            return lastLine;
        } catch (IOException e) {
            System.err.println("Error reading last message from log: " + e.getMessage());
            return null;
        }
    }

    // Add this helper method to check if nickname is already in use
    private boolean isNicknameAlreadyConnected(String nickname) {
        return connectedClients.values().contains(nickname);
    }
}

