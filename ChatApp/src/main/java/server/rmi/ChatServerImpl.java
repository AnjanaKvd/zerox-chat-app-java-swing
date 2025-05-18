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
            
            
            Chat chat = chatDAO.findById(chatId);
            if (chat != null) {
                logMessageToChat(formattedMessage, chat);
            }
            
            
            broadcastMessageToChat(formattedMessage, chatId);
            
            
            if (message.equalsIgnoreCase("Bye") && senderClient != null) {
                
            }
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
        
        connectedClients.put(client, nickname);
        
        
        chatRooms.computeIfAbsent(chatId, k -> new ArrayList<>()).add(client);
        clientChatMap.put(client, chatId);
        
        
        Chat chat = chatDAO.findById(chatId);
        if (chat != null) {
            
            String joinMessage = nickname + " has joined : " + getCurrentTime();
            
            
            logMessageToChat(joinMessage, chat);
            
            
            broadcastMessageToChat(joinMessage, chatId);
            
            
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
        String[] userList = connectedClients.values().toArray(new String[0]);
        
        for (ChatClient client : connectedClients.keySet()) {
            try {
                client.updateUserList(userList);
            } catch (RemoteException e) {
                
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
        
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
        
        String logFile = chat.getLogFile();
        if (logFile == null || logFile.isEmpty()) {
            
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String chatName = chat.getName() != null ? chat.getName() : "Chat_" + chat.getId();
            logFile = "logs/chat_" + chatName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".txt";
            
            
            chat.setLogFile(logFile);
            chatDAO.saveChat(chat);
            
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                writer.write("Chat '" + chatName + "' created at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.newLine();
                writer.write("Created by admin: " + (chat.getAdmin() != null ? chat.getAdmin().getUsername() : "system"));
                writer.newLine();
                writer.write("-------------------------------------------");
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error creating chat log file: " + e.getMessage());
                return; 
            }
        }
        
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to chat log: " + e.getMessage());
        }
    }
}
