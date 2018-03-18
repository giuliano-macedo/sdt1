package hangman;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.net.UnknownHostException;

public interface HangmanMaster extends Remote {
	void join() throws RemoteException;
	void exit() throws RemoteException;
}