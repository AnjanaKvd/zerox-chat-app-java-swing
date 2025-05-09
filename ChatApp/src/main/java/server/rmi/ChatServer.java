package server.rmi;

import server.observer.ChatClient;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatServer extends Remote {
    void registerClient(ChatClient client, String nickname) throws RemoteException;
    void registerClientToChat(ChatClient client, String nickname, int chatId) throws RemoteException;
    void sendMessage(String message, String nickname) throws RemoteException;
    void removeClient(ChatClient client, String nickname) throws RemoteException;
}
