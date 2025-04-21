package client.services;

import client.gui.ChatWindow;
import server.observer.ChatClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

public class ChatClientImpl extends UnicastRemoteObject implements ChatClient {
    private ChatWindow chatWindow;
    
    public ChatClientImpl() throws RemoteException {
        super();
    }
    
    public void setChatWindow(ChatWindow chatWindow) {
        this.chatWindow = chatWindow;
    }
    
    @Override
    public void receiveMessage(String message) throws RemoteException {
        if (chatWindow != null) {
            chatWindow.appendToChatArea(message);
        }
    }
    
    @Override
    public void updateUserList(String[] users) throws RemoteException {
        if (chatWindow != null) {
            chatWindow.updateUserList(users);
        }
    }
    
    @Override
    public void notifyChatStarted(String time) throws RemoteException {
        if (chatWindow != null) {
            chatWindow.appendToChatArea("Chat started at: " + time);
        }
    }
    
    @Override
    public void notifyChatEnded(String time) throws RemoteException {
        if (chatWindow != null) {
            chatWindow.appendToChatArea("Chat ended at: " + time);
        }
    }
}
