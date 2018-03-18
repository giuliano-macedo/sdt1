package hangman;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Slave implements HangmanSlave{
	static void err(String msg){
		System.err.println(msg);
	    System.exit(-1);
	}
	static class ServerInfo{
        Registry registry;
        HangmanMaster server;
        // int id;
        // Hangman.HangmanInfo hi;
    }
	static ServerInfo connectTo(String host)throws Exception{
        ServerInfo ans=new ServerInfo();
        try{
            ans.registry = LocateRegistry.getRegistry(host,4243);
            ans.server = (HangmanMaster) ans.registry.lookup("hangmanMaster");
            // ans.hi = ans.server.getHangmanInfo();
            // ans.id = ans.server.connect();
        
        }catch(Exception e){
            throw e;
        }
        return ans;
    }
    //rpcs
    void addWords(List<String> words) throws RemoteException;
    List<String> removeWords(int noWords) throws RemoteException;
    int getNoWords() throws RemoteException;//debug

    int getWord() throws RemoteException;
    List<Integer> guess(char c) throws RemoteException;
    //
	public static void main(String[] args){
		// if(args.length!=1){
		// 	System.out.println("[USO] slave [ip do servidor]");
		// }
		// String svIp=args[0];
		ServerInfo svInfo=new ServerInfo();
		try {svInfo=connectTo("127.0.0.1");}
        catch(Exception e){err(e.toString());}
        
        try{svInfo.server.join();}
        catch(Exception e){err(e.toString());}
	}
}