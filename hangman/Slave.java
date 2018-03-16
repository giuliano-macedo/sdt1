package hangman;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Slave{
	static void err(String msg){
		System.err.println(msg);
	    System.exit(-1);
	}
	static class ServerInfo{
        Registry registry;
        HangmanSlaveServer server;
        // int id;
        // Hangman.HangmanInfo hi;
    }
	static ServerInfo connectTo(String host)throws Exception{
        ServerInfo ans=new ServerInfo();
        try{
            ans.registry = LocateRegistry.getRegistry(host,8888);
            ans.server = (HangmanSlaveServer) ans.registry.lookup("hangmanSlaveServer");
            // ans.hi = ans.server.getHangmanInfo();
            // ans.id = ans.server.connect();
        
        }catch(Exception e){
            throw e;
        }
        return ans;
    }
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