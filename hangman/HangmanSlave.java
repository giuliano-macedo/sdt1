package hangman;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.net.UnknownHostException;

import java.util.List;

public interface HangmanSlave extends Remote {
	void addWords(List<String> words) throws RemoteException;
	List<String> removeWords(int noWords) throws RemoteException;
	int getNoWords() throws RemoteException;//debug

	int getWord() throws RemoteException;
	List<Integer> guess(char c) throws RemoteException;
}