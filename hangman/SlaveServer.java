package hangman;

import java.rmi.server.RemoteServer;
import java.rmi.RemoteException;

public class SlaveServer extends RemoteServer implements HangmanSlaveServer{
	static SlaveStatus{
		
	}
	HashMap<Byte[],Integer> slavesId;
	HashMap<Byte[],SlaveStatus> clientsStatus;
	public SlaveServer(){

	}
	public void join() throws RemoteException{
		String ip="";
		try{ip=getClientHost();}
		catch(Exception e){throw new RemoteException(e.toString());}
	}
	public void exit() throws RemoteException{

	}
}