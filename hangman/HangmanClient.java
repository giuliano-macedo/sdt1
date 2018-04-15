package hangman;
import java.rmi.RemoteException;
import java.rmi.Remote;
public interface HangmanClient extends Remote{
	int beat() throws RemoteException;
	void exit() throws RemoteException;
	void sendHInfo(Hangman.HangmanInfo hinfo)throws RemoteException;
}