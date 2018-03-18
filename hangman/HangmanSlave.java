package hangman;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.net.UnknownHostException;

import java.util.List;
import java.util.ArrayList;

public interface HangmanSlave extends Remote {
	void addWords(List<String> w) throws RemoteException;
	List<String> removeWords(int noWords) throws RemoteException;
	int getNoWords() throws RemoteException;//debug

	int getWord(int id) throws RemoteException;
	List<Integer> guess(int id,char c) throws RemoteException;
}