package hangman;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public interface HangmanMaster extends Remote {
	void join() throws RemoteException;
	void exit(ArrayList<String> words) throws RemoteException;
	int beat() throws RemoteException;
}