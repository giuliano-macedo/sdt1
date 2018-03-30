package hangman;

import java.rmi.server.RemoteServer;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

import java.net.InetAddress;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
public class Master extends RemoteServer implements HangmanMaster{
	boolean isJoining=false;
	boolean isKicking=false;
	HashMap<Byte[],Integer> slavesId;
	ArrayList<HangmanSlaveInfo> slaves;
	ArrayList<String> words;
	Server server;
	int maxLives;
	static class HeartBeatSlave implements Runnable{
        HangmanSlave server;
        Master obj;
        Byte[] clientIp;
        public HeartBeatSlave(Master o,HangmanSlave sv,Byte[] ip){
            server=sv;
            obj=o;
            clientIp=ip;
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
                    Server.serverMsg("conexão perdida com um escravo ");
                    obj.recoverWords(clientIp);
                    obj.kickSlave(clientIp);
                    return;
                }
            }
        }
    }
	int totalNoWords;
	static class HangmanSlaveInfo{
		HangmanSlave server;
		Registry registry;
	}
	public Master(ArrayList<String> w,int lives,Server s){
		server=s;
		totalNoWords=w.size();
		maxLives=lives;
		words=w;
		slavesId=new HashMap<Byte[],Integer>();
		slaves=new ArrayList<HangmanSlaveInfo>();
		// slavesNoWord=new HashMap<Byte[],Integer>();
	}
	public static Byte[] getAddr(String ip){
		byte[] in=new byte[0];
		try{in=InetAddress.getByName(ip).getAddress​();}
		catch(Exception e){
			//todo
		}
		Byte[] ans=new Byte[in.length];
		for(int i=0;i<in.length;i++){
			ans[i]=in[i];
		}
		return ans;
	}
	void kickSlave(Byte[] ip){
		while(isKicking||isJoining){
			try{
				Thread.sleep(100);
			}
			catch(Exception e){return;}
		}
		isKicking=true;
		int id=slavesId.get(ip);
		HangmanSlaveInfo hi=slaves.get(id);
		Server.serverMsg("Kickando escravo id:"+id);
		int s=slaves.size();
		for(Byte[] k:slavesId.keySet()){
			Integer v=slavesId.get(k);
			if(v>id){
				slavesId.put(k,v-1);
			}
		}
		server.disassociateSlave(hi.server);
		slavesId.remove(ip);
		slaves.remove(id);
		isKicking=false;
	}
	public void distributeWords()throws RemoteException{
		int expectedSize=totalNoWords/(slaves.size()+1);
		
		int ws=words.size();
		slaves.get(0).server.addWords(
			new ArrayList<String>(
				words.subList(expectedSize,ws)));
		words.subList(expectedSize,ws).clear();
		
		if(slaves.size()==1)return;
		//todo
		ArrayList<String> c=slaves.get(0).server.reduceWords(expectedSize);
		int s=slaves.size();
		int a=0;
		HangmanSlaveInfo si;
		for(int i=1;i<s;i++){
			si=slaves.get(i);
			a=si.server.addWords(c);
			if(i!=s-1)c=si.server.reduceWords(expectedSize);
		}
	}
	//rpc
	public void join() throws RemoteException{
		while(isJoining||isKicking){
			try{
				Thread.sleep(100);
			}
			catch(Exception e){return;}
		}
		isJoining=true;
		String ip="";
        Byte[] ipAddr=null;
        int id=slaves.size();
		try{
			ip=getClientHost();
			ipAddr=getAddr(ip);
		}
		catch(Exception e){
			isJoining=false;
			throw new RemoteException(e.toString());
		}
		
		HangmanSlaveInfo si=new HangmanSlaveInfo();
		slaves.add(si);

		try{
            si.registry = LocateRegistry.getRegistry(ip.toString(),4244);
            si.server = (HangmanSlave) si.registry.lookup("hangmanSlave");
		}
		catch(Exception e){
			isJoining=false;
			throw new RemoteException(e.toString());
		}

		Runnable r=new HeartBeatSlave(this,si.server,ipAddr);
		new Thread(r).start();
		si.server.setLives(maxLives);
		try{
			distributeWords();
		}
		catch(Exception e){
			isJoining=false;
			Server.serverMsg("Falha ao distribuir palavras");
			System.exit(0);
		}
		slavesId.put(ipAddr,id);
		Server.serverMsg(ip.toString()+" se conectou como escravo");
		isJoining=false;
	}
	public void fillWordsBack(int id,ArrayList<String> w){
		server.serverMsg("passando palavras :"+w.toString()+" para tras");
		if(id==0){
			server.words.addAll(server.words.size(),w);
		}
		else{
			try{
				slaves.get(id-1).server.addWords(w);
			}
			catch(Exception e){
				System.out.println("kickando escravo "+(id-1)+" pois não adicionou palavras de um escravo que estava saindo");
				// kickSlave(slaves.get(id-1));
			}
		}
	}
	public void recoverWords(Byte[] ip){
		int id=slavesId.get(ip);
		int expectedSize=(int)Math.round(totalNoWords/(slaves.size()+1));
		int startOffset=expectedSize*id;
		int endOffset=0;
		if(id!=slaves.size()-1){
			endOffset=startOffset+expectedSize;
		}
		else{
			endOffset=totalNoWords;
		}
		ArrayList<String> w=server.cacher.get(startOffset,endOffset);
		fillWordsBack(id,w);
	}
	public void exit(ArrayList<String> w) throws RemoteException{
		String ip="";
        Byte[] ipAddr=null;
        int id=0;
		try{
			ip=getClientHost();
			ipAddr=getAddr(ip);
			id=slavesId.get(ipAddr);
		}
		catch(Exception e){throw new RemoteException(e.toString());}
		fillWordsBack(id,w);
        kickSlave(ipAddr);
	}
	public int beat() throws RemoteException{
		return 1;
	}
	//
}