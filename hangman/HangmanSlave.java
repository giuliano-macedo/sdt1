package hangman;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.net.UnknownHostException;

import java.util.List;
import java.util.ArrayList;

public interface HangmanSlave extends Remote {
	void setLives(int lives) throws RemoteException;
	int addWords(ArrayList<String> w) throws RemoteException;
	void removeWords(int noWords) throws RemoteException;
	ArrayList<String> reduceWords(int expected)throws RemoteException;
	ArrayList<String> getWords() throws RemoteException;//debug

	int getWord(int id) throws RemoteException;
	ArrayList<Integer> guess(int id,char c) throws RemoteException;
	void removeClient(int id)throws RemoteException;
	int beat()throws RemoteException;
}