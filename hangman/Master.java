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
	//rpc
	public void join() throws RemoteException{
		String ip="";
        Byte[] ipAddr=null;
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

	}
	//
}