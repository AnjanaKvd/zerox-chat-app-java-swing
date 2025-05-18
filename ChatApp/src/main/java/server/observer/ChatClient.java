package server.observer;

import model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatClient extends Remote {
    void receiveMessage(String message) throws RemoteException;
    void updateUserList(String[] users) throws RemoteException;

    void notifyChatStarted(String time) throws RemoteException;
    void notifyChatEnded(String time) throws RemoteException;
}
