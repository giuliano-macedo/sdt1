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
	HashMap<Byte[],Integer> slavesId;
	ArrayList<HangmanSlaveInfo> slaves;
	int maxLives;
	public ArrayList<String> words;
	static class HeartBeatSlave implements Runnable{
        HangmanSlave server;
        Server obj;
        int clientId;
        public HeartBeatSlave(Server o,HangmanSlave sv,int id){
            server=sv;
            obj=o;
            clientId=id;
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
                    serverMsg("conexão perdida com escravo id:"+clientId);
                    obj.kickSlave(clientId);
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
	public Master(ArrayList<String> w,int lives){
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
	void kickSlave(Byte[] ip,int id){
		System.out.println("Kickando escravo "+ip.toString());
		slaves.remove(id);    //not sure
		slavesId.remove(ipAddr);//too
	}
	//rpc
	public void join() throws RemoteException{
		String ip="";
        Byte[] ipAddr=null;
        int id=slaves.size();
		try{
			ip=getClientHost();
			ipAddr=getAddr(ip);
		}
		catch(Exception e){throw new RemoteException(e.toString());}
		
		HangmanSlaveInfo si=new HangmanSlaveInfo();
		slaves.add(si);

		try{
            si.registry = LocateRegistry.getRegistry(ip.toString(),4244);
            si.server = (HangmanSlave) si.registry.lookup("hangmanSlave");
		}
		catch(Exception e){throw new RemoteException(e.toString());}

		r=new HeartBeatSlave(this,si.server,id);
		new Thread(r).start();

		int expectedSize=totalNoWords/(slaves.size()+1);
		try{
			si.server.setLives(maxLives);
			si.server.addWords(new ArrayList<String>(words.subList(0,expectedSize)));
		}
		catch(Exception e){
			throw new RemoteException("Falha ao enviar palavras "+e.toString());
		}
		words.subList(0,expectedSize).clear();
		int s=slaves.size()-1;
		for(int i=0;i<s;i++){
			try{
				si.server.addWords(new ArrayList<String>(slaves.get(i).server.removeWords(expectedSize)));
			}
			catch(Exception e){
				throw new RemoteException("Falha ao enviar palavras");
			}
		}
		slavesId.put(ipAddr,slaves.size());
		System.out.print("\r"+ip.toString()+" se conectou como escravo\n>");
	}
	public void exit() throws RemoteException{
		String ip="";
        Byte[] ipAddr=null;
		try{
			ip=getClientHost();
			ipAddr=getAddr(ip);
		}
		catch(Exception e){throw new RemoteException(e.toString());}
        int id=slavesId.get(ipAddr);
        kickSlave(id,ipAddr);
	}
	public int beat() throws RemoteException{
		return 1;
	}
	//
}