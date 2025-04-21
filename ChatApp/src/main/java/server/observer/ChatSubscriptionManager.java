package server.observer;

import model.User;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatSubscriptionManager {
    private final Map<Integer, List<User>> chatSubscriptions; // chatId -> subscribers
    
    public ChatSubscriptionManager() {
        chatSubscriptions = new HashMap<>();
    }
    
    public void subscribeUserToChat(int chatId, User user) {
        chatSubscriptions.computeIfAbsent(chatId, k -> new ArrayList<>()).add(user);
    }
    
    public void unsubscribeUserFromChat(int chatId, User user) {
        if (chatSubscriptions.containsKey(chatId)) {
            chatSubscriptions.get(chatId).remove(user);
        }
    }
    
    public List<User> getSubscribedUsers(int chatId) {
        return chatSubscriptions.getOrDefault(chatId, new ArrayList<>());
    }
    
    public boolean isUserSubscribed(int chatId, User user) {
        List<User> subscribers = chatSubscriptions.get(chatId);
        return subscribers != null && subscribers.contains(user);
    }
    
    public void notifySubscribedUsers(int chatId, String message, Map<String, ChatClient> activeClients) {
        List<User> subscribers = getSubscribedUsers(chatId);
        for (User user : subscribers) {
            // Check if user is currently online and has an active client
            ChatClient client = activeClients.get(user.getUsername());
            if (client != null) {
                try {
                    client.receiveMessage(message);
                } catch (RemoteException e) {
                    // Handle exception, perhaps by removing the client
                    activeClients.remove(user.getUsername());
                }
            }
        }
    }
    
    public void notifyChatStarted(int chatId, String time, Map<String, ChatClient> activeClients) {
        List<User> subscribers = getSubscribedUsers(chatId);
        for (User user : subscribers) {
            // Check if user is currently online and has an active client
            ChatClient client = activeClients.get(user.getUsername());
            if (client != null) {
                try {
                    client.notifyChatStarted(time);
                } catch (RemoteException e) {
                    // Handle exception, perhaps by removing the client
                    activeClients.remove(user.getUsername());
                }
            }
        }
    }
} 