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
	ArrayList<String> words;
	int totalNoWords;
	static class HangmanSlaveInfo{
		HangmanSlave server;
		Registry registry;
	}
	public Master(ArrayList<String> w){
		totalNoWords=w.size();
		words=w;
		slavesId=new HashMap<Byte[],Integer>();
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
            si.registry = LocateRegistry.getRegistry(ip,4244);
            si.server = (HangmanSlave) si.registry.lookup("slaveClient");
		}
		catch(Exception e){throw new RemoteException(e.toString());}
		int expectedSize=totalNoWords/(slaves.size()+1);
		
		try{
			si.server.addWords(words.subList(0,expectedSize));
		}
		catch(Exception e){
			//cancel joining process
		}
		words.subList(0,expectedSize);
		int s=slaves.size()-1;
		for(int i=0;i<s;i++){
			try{
				si.server.addWords(slaves.get(i).server.removeWords(expectedSize));
			}
			catch(Exception e){
				//TODO
			}

		}
		slavesId.put(ipAddr,slaves.size());
	}
	public void exit() throws RemoteException{

	}
	//
}