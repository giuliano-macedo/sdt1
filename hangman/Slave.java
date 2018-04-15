package hangman;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteServer;

public class Slave extends RemoteServer implements HangmanSlave{
    static class HeartBeat implements Runnable{
        HangmanMaster server;
        public HeartBeat(HangmanMaster sv){
            server=sv;
        }
        public void run(){
            while(true){
                try{Thread.sleep(500);}
                catch(Exception e){}
                try{
                    int n=server.beat();
                    if(n!=1)throw new Exception("Beat errado");
                }
                catch(Exception e){
                    System.out.println("Conexão com servidor perdida");
                    System.exit(0);
                }
            }
        }
    }
    static class ServerInfo{
        Registry registry;
        HangmanMaster server;
    }
    int maxLives;
    ArrayList<String> words;
    HashMap<Integer,Player> players;
    public Slave(){
        players=new HashMap<Integer,Player>();
        words=new ArrayList<String>();
    }
    static void err(String msg){
        System.err.println(msg);
        System.exit(-1);
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
    public ArrayList<String> reduceWords(int expected)throws RemoteException{
        int s=words.size();
        if(expected>=s)return new ArrayList<String>();
        ArrayList<String> ans=new ArrayList<String>(words.subList(expected,s));
        words.subList(expected,s).clear();
        System.out.println("Reduzindo para "+words.toString()+" restantes "+ans.toString());
        return ans;
    }
    public void addWords(ArrayList<String> w){
        System.out.print("adicionando "+w.toString());
        words.addAll(0,w);
        System.out.println(" ="+words.toString());
    }
    public void setWords(ArrayList<String> w){
        words.clear();
        addWords(w);
    }
    public void appendWords(ArrayList<String> w){
        System.out.print("adicionando para o final "+w.toString());
        words.addAll(words.size(),w);
        System.out.println(" ="+words.toString());
    }
    public void removeWords(int noWords) throws RemoteException{
        if(words.size()==0)return;
        if(noWords==words.size()){
            throw new RemoteException("Zerando palavras de um escravo");
        }
        int s=words.size();
        words.subList(s-noWords+1,s).clear();

    }
    public ArrayList<String> getWords(){
        return words;
    }//debug

    public int getWordsSize(){
        return words.size();
    }

    public int getWord(int id){
        String c=words.get(new Random().nextInt(words.size()));
        System.out.println("chosen "+c);
        players.put(id,new Player().setLives(maxLives).setWord(c));
        return c.length();
    }
    public ArrayList<Integer> guess(int id,char c)throws RemoteException{
        ArrayList<Integer> ans;
        c=Character.toLowerCase(c);
        Player p=players.get(id);
        try{
            ans=p.guess(c);
        }
        catch(Exception e){throw new RemoteException(e.toString());}
        if(!ans.isEmpty()){
            switch(ans.get(0)){
                case -2:
                    System.out.println("WRONG");
                    System.out.printf("%s got word wrong! R:%d W:%d\n",id,p.scoreR,p.scoreW);
                    break;
                case -3:
                    System.out.printf("%s got word right! R:%d W:%d\n",id,p.scoreR,p.scoreW);
                    break;
            }
        }
        return ans;
    }
    public void setLives(int lives){
        maxLives=lives;
    }
    public void removeClient(int id){
        players.remove(id);
    }
    public int beat(){
        return 1;
    }
    //
	public static void main(String[] args){
		if(args.length!=1){
			System.out.println("[USO] slave [ip do servidor]");
		}
		String svIp=args[0];
		ServerInfo svInfo=new ServerInfo();
		try {svInfo=connectTo(svIp);}
        catch(Exception e){err(e.toString());}
        
        Registry registry;
        Slave obj= new Slave();
        try{
            HangmanSlave stub = (HangmanSlave) UnicastRemoteObject.exportObject(obj, 0);
            registry = LocateRegistry.createRegistry(4244);
            registry.bind("hangmanSlave", stub);
        }catch(Exception e){
            System.err.println("Server exception: " + e.toString());
            System.exit(-1);
        }

        Runnable r=new HeartBeat(svInfo.server);
        new Thread(r).start();

        try{svInfo.server.join();}
        catch(Exception e){err("Server :"+e.toString());}
	}
}