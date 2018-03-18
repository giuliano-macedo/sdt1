package hangman;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import java.rmi.server.RemoteServer;

public class Slave extends RemoteServer implements HangmanSlave{
    ArrayList<String> words;
    HashMap<Integer,String> clientsWord;
    public Slave(){
        words=new ArrayList<String>();
    }
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
        
        }catch(Exception e){
            throw e;
        }
        return ans;
    }
    //rpcs
    public void addWords(List<String> w){
        words.addAll(words.size(),w);
    }
    public List<String> removeWords(int noWords) {
        if(noWords==words.size()){
            //throw err
        }
        List<String> ans=words.subList(0,noWords);
        words.subList(0,noWords).clear();
        return ans;
    }
    public int getNoWords() {
        return words.size();
    }//debug

    public int getWord(int id){
        return 1; //TODO
    }
    public List<Integer> guess(int id,char c){
        List<Integer> ans=null;
        //TODO
        return ans;
    }
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