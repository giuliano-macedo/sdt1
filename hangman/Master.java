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
                    masterMsg("conexão perdida com um escravo ");
                    if(obj.isWordsConsistent(clientIp)){
                    	masterMsg("Recuperando palavras de um escravo");
                    	obj.recoverWords(clientIp);
                    }
                    else{
                    	masterMsg("Recuperando palavras do zero");
                    	obj.recoverFromScratch(clientIp);
                    }
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
	public Master(int lives,Server s){
		server=s;
		totalNoWords=server.words.size();
		maxLives=lives;
		slavesId=new HashMap<Byte[],Integer>();
		slaves=new ArrayList<HangmanSlaveInfo>();
		// slavesNoWord=new HashMap<Byte[],Integer>();
	}
	public static void masterMsg(String msg){
		System.out.printf("\r[MESTRE] %s\n>",msg);
	}
	public static Byte[] getAddr(String ip){
		byte[] in=new byte[0];
		try{in=InetAddress.getByName(ip).getAddress​();}
		catch(Exception e){
			System.err.println("Falha ao obter endereço ip de um escravo");
			System.exit(-1);
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
		masterMsg("Kickando escravo id:"+id);
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
		
		int ws=server.words.size();
		slaves.get(0).server.addWords(
			new ArrayList<String>(
				server.words.subList(expectedSize,ws)));
		server.words.subList(expectedSize,ws).clear();
		
		if(slaves.size()==1)return;
		ArrayList<String> c=slaves.get(0).server.reduceWords(expectedSize);
		int s=slaves.size();
		HangmanSlaveInfo si;
		for(int i=1;i<s;i++){
			si=slaves.get(i);
			si.server.addWords(c);
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
			masterMsg("Falha ao distribuir palavras");
			System.exit(0);
		}
		slavesId.put(ipAddr,id);
		masterMsg(ip.toString()+" se conectou como escravo");
		isJoining=false;
	}
	public void fillWordsBack(int id,ArrayList<String> w){
		masterMsg("passando palavras :"+w.toString()+" para tras");
		if(id==0){
			masterMsg("adicionando para o servidor");
			server.words.addAll(server.words.size(),w);
		}
		else{
			try{
				slaves.get(id-1).server.appendWords(w);
			}
			catch(Exception e){
				System.out.println("kickando escravo "+(id-1)+" pois não adicionou palavras de um escravo que estava saindo");
				for(Byte[] itid:slavesId.keySet()){
					if(slavesId.get(itid)==(id-1)){
						kickSlave(itid);
						break;
					}
				}
				fillWordsBack(0,w);
			}
		}
	}
	public boolean isWordsConsistent(Byte[] excIp){
		int excId=slavesId.get(excIp);
		int c=server.words.size();
		int ac=0;
		int s=slaves.size();
		int ex=Math.round(totalNoWords/((slaves.size()+1)));
		if(c!=ex)return false;
		for(int i=0;i<s;i++){
			if(i==excId)continue;
			try{
				ac=slaves.get(i).server.getWordsSize();
			}
			catch(Exception e){
				masterMsg("Falha ao verificar tamanho do dicionario de um escravo, kickando...");
				for(Byte[] itid:slavesId.keySet()){
					if(slavesId.get(itid)==i){
						kickSlave(itid);
						break;
					}
				}
				return false;
			}
			if(i!=s-1){
				if(ac!=c)return false;
			}
			else{
				int temp=totalNoWords%ex;
				temp+=ex;
				if(ac!=temp)return false;
			}
		}
		return true;
	}

	public void recoverFromScratch(Byte[] excIp){
		int excId=slavesId.get(excIp);
		WordCacher c=server.cacher;
		int ex=(int)Math.round(totalNoWords/(slaves.size()));
		server.words=c.get(0,ex);
		int s=slaves.size();
		s++;
		for(int i=1;i<s;i++){
			if(i-1==excId)continue;
			int f=(i==s-1)?totalNoWords:(i+1)*ex;
			try{
				slaves.get(i-1).server.setWords(c.get((i*ex),f));
			}
			catch(Exception e){
				masterMsg("Falha ao adicionar palavra a um escravo, kickando...");
				for(Byte[] itid:slavesId.keySet()){
					if(slavesId.get(itid)==i){
						kickSlave(itid);
						break;
					}
				}
				recoverFromScratch(excIp);
			}
		}
	}
	public void recoverWords(Byte[] ip){
		int id=slavesId.get(ip);
		int expectedSize=(int)Math.round(totalNoWords/(slaves.size()+1));
		int startOffset=expectedSize*(id+1);
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