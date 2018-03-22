package hangman;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.net.UnknownHostException;

import java.util.ArrayList;

public interface Hangman extends Remote {
	static class HangmanInfo implements java.io.Serializable{
		String topic;
		int minWord;
		int maxWord;
		int lives;
	}
    int connect() throws ServerNotActiveException,UnknownHostException,RemoteException;
    Hangman.HangmanInfo getHangmanInfo() throws RemoteException;
    int getWord() throws RemoteException;
    ArrayList<Integer> guess(char c) throws RemoteException;
    ArrayList<Integer> getScore() throws RemoteException;
    int beat() throws RemoteException;
}