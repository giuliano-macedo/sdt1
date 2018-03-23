package hangman;
        
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.RemoteServer;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Scanner;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Collections;


// import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Server extends RemoteServer implements Hangman {
    Master master;
    HashMap<InetAddress,Integer> clientsId;
    HashMap<Integer,ClientInfo> clientsInfo;
    HashMap<Integer,HangmanSlave> associatedSlaves;
    HashMap<InetAddress,Player> players;
    WordCacher cacher;
    String topic;
    int dictLen;
    int minWord;
    int maxWord;
    int noLives;
    
    ArrayList<String> words;

    Hangman.HangmanInfo HInfo;
    ConcurrentLinkedQueue<Integer> idPool;
    static public int IDPOOL_SIZE = 128;
    static class ClientInfo{
        ClientInfo(){}
        HeartBeatClient hb;
        HangmanClient server;
    }
    static class HeartBeatClient implements Runnable{
        HangmanClient server;
        Server obj;
        int clientId;
        boolean stop=false;
        public HeartBeatClient(Server o,HangmanClient sv,int id){
            server=sv;
            obj=o;
            clientId=id;
        }
        public void run(){
            while(!stop){
                try{Thread.sleep(500);}
                catch(Exception e){}
                try{
                    if(stop)throw new Exception("parando");
                    int n=server.beat();
                    if(n!=1)throw new Exception("Beat errado");
                }
                catch(Exception e){
                    serverMsg("conexão perdida com cliente id:"+clientId);
                    obj.kickClient(clientId);
                    return;
                }
            }
        }
    }
    public Server(String args[]){
        if(args.length!=3){
            System.err.printf("[Uso] server [caminho para arquivo de dicionario] [topico] [número de vidas]\n");
            System.exit(1);
        }
        String dictPath=args[0];
        topic=args[1];
        noLives=Integer.parseInt(args[2]);
        try{
            FileReader fr = new FileReader(dictPath);
            BufferedReader br=new BufferedReader(fr);
            String l="";
            minWord=Integer.MAX_VALUE;
            maxWord=0;
            dictLen=0;
            words=new ArrayList<String>();
            while((l = br.readLine()) != null) {
                int len=l.length();
                if(len<minWord)minWord=len;
                if(len>maxWord)maxWord=len;
                words.add(l);
                dictLen++;
            }
            br.mark(0);
            cacher=new WordCacher(br);
            if(dictLen==0)throw new Exception("Arquivo vazio");
        }
        catch(Exception e){
            System.err.printf("não foi possivel abrir %s pois %s\n",dictPath,e.toString());
            System.exit(1);
        }
        master= new Master(words,noLives,this);
        System.out.printf("no de palavras carregadas:%d\n",dictLen);
        System.out.printf("maior palavra:%d menor palavra:%d\n",maxWord,minWord);
        idPool=new ConcurrentLinkedQueue<Integer>();
        LinkedList<Integer> temp=new LinkedList<Integer>();
        for(int i=1;i<IDPOOL_SIZE+1;i++){
            temp.push(i);
        }
        Collections.shuffle(temp);
        for(int i=0;i<IDPOOL_SIZE;i++){
            idPool.add(temp.pop());
        }

        HInfo = new Hangman.HangmanInfo();
        HInfo.topic=topic;
        HInfo.minWord=minWord;
        HInfo.maxWord=maxWord;
        HInfo.lives=noLives;

        clientsId=new HashMap<InetAddress,Integer>();
        clientsInfo=new HashMap<Integer,ClientInfo>();
        players=new HashMap<InetAddress,Player>();
        associatedSlaves=new HashMap<Integer,HangmanSlave>();
    }
    static void printClientList(HashMap<InetAddress,Integer> l){
        System.out.printf("%-11s %-32s\n","ip","id");
        for(Map.Entry<InetAddress,Integer> e : l.entrySet()){
            System.out.printf("%-11s %-32d\n",
                e.getKey().toString(),
                e.getValue());
        }
    }
    static void printSlavesList(HashMap<Byte[],Integer> l){
        System.out.printf("%-11s %-32s\n","ip","id");
        for(Map.Entry<Byte[],Integer> e : l.entrySet()){
            Byte[] temp=e.getKey();
            String ip=String.format("%d.%d.%d.%d",temp[0],temp[1],temp[2],temp[3]);
            System.out.printf("%-11s %-32d\n",ip,e.getValue());
        }
    }
    static void availableCommands(){
       System.out.println("Comandos disponiveis");
       System.out.println("clients [mostre lista de clientes]");
       System.out.println("slaves [mostra lista de escravos]");
       System.out.println("help [mostra essa lista]");
       System.out.println("stop [para o servidor]");
    }
    static void serverMsg(String msg){
        System.out.printf("\r[SERVIDOR] %s\n>",msg);

    }
    private String choseWord(){
        return words.get(new Random().nextInt(words.size()));
    }
    public Hangman.HangmanInfo getHangmanInfo(){
        return HInfo;
    }
    public void disassociateSlave(HangmanSlave hg){
        for(Integer id:associatedSlaves.keySet()){
            if((associatedSlaves.get(id))==hg){
                ClientInfo ci=clientsInfo.get(id);
                try{
                    ci.server.exit();
                }
                catch(Exception e){}
                ci.hb.stop=true;
                associatedSlaves.remove(id);
            }
        }

    }
    public void kickClient(int id){
        InetAddress ipAddr=null;
        //BAD ITERATION
        for (InetAddress o : clientsId.keySet()) {
          if (clientsId.get(o) == id){
            ipAddr=o;
          }
        }
        //
        if(ipAddr==null)return;
        serverMsg("Kickando "+ipAddr.toString());
        clientsId.remove(ipAddr);
        HangmanSlave s;
        if((s=associatedSlaves.get(id))!=null){
            try{
                s.removeClient(id);
            }
            catch(Exception e){}
        }
        else{
            players.remove(ipAddr);
        }
        idPool.add(id);
    }
    //rpcs
    public int connect()throws ServerNotActiveException,UnknownHostException{
        // if(idPool.empty())throw new Exception("Acabou ids"); //TODO
        Registry registry =null;
        Runnable r=null;
        String ip=getClientHost();
        int id=idPool.poll();
        ClientInfo ci=new ClientInfo();
        try{
            registry=LocateRegistry.getRegistry(ip.toString(),4245);
            ci.server=(HangmanClient)registry.lookup("hangmanClient");
            ci.hb=new HeartBeatClient(this,ci.server,id);
            r=ci.hb;
        }
        catch(Exception e){
            serverMsg("Falha ao se conectar conectar "+ip.toString()+" "+e.toString());
            idPool.add(id);
        }
        new Thread(r).start();
        clientsInfo.put(id,ci);
        InetAddress ipAddr=InetAddress.getByName(ip);
        clientsId.put(ipAddr,id);
        players.put(ipAddr,new Player());

        serverMsg(ip+" se conectou");
        return id;
    }
    public int getWord()throws RemoteException{
        String ip="";
        InetAddress ipAddr=null;
        try {
            ip=getClientHost();
            ipAddr=InetAddress.getByName(ip);
        }
        catch(Exception e){}//todo
        int id=clientsId.get(ipAddr);
        // if(!master.slaves.isEmpty()&&new Random().nextInt(4)==0){
        if(!master.slaves.isEmpty()){
            int ans=0;
            int i=new Random().nextInt(master.slaves.size());
            HangmanSlave hs=master.slaves.get(i).server;
            if(associatedSlaves.get(id)!=null){
                throw new RemoteException("Id já associado");
            }
            associatedSlaves.put(id,hs);
            
            try{
                ans=hs.getWord(id);
            }
            catch(Exception e){throw new RemoteException("Server:"+e.toString());}
            serverMsg(String.format("Escolhido algo para %s com escravo %d\n",ip,i));
            return ans;
        }
        if(players.get(ipAddr).word!=""){
            throw new RemoteException("Palavra já escolhida para cliente");
        }

        String chose=choseWord();
        int ans=chose.length();
        serverMsg(String.format("chose %s to %s\n",chose,ip));
        players.get(ipAddr).setLives(noLives).setWord(chose);
        
        return ans;
    }
    public ArrayList<Integer> getScore()throws RemoteException{
        InetAddress ip=null;
        try{
            ip=InetAddress.getByName(getClientHost());
        }
        catch(Exception e){}//todo
        ArrayList<Integer> ans= new ArrayList<Integer>(2);
        Player p=players.get(ip);
        ans.add(p.scoreR);
        ans.add(p.scoreW);
        return ans;
    }
    public ArrayList<Integer> guess(char c)throws RemoteException{
        c=Character.toLowerCase(c);
        ArrayList<Integer> ans=null;
        String word="";
        InetAddress ip=null;
        Player p=null;
        
        try{
            ip=InetAddress.getByName(getClientHost());
            p=players.get(ip);
        }
        catch(Exception e){}//todo
        int id=clientsId.get(ip);
        HangmanSlave hs;
        if((hs=associatedSlaves.get(id))!=null){
            try{
                ans=hs.guess(id,c);
            }
            catch(Exception e){throw new RemoteException(e.toString());}
            if(!ans.isEmpty()){
                switch(ans.get(0)){
                    case -2:
                        p.scoreW++;
                        associatedSlaves.remove(id);
                        System.out.printf("%s PERDEU! R:%d W:%d\n",ip.toString(),p.scoreR,p.scoreW);
                        break;
                    case -3:
                        System.out.printf("%s ACERTOU! R:%d W:%d\n",ip.toString(),p.scoreR,p.scoreW);
                        p.scoreR++;
                        ans.remove((int)0);
                        associatedSlaves.remove(id);
                        break;
                }
            }
            return ans;
        }
        try{
            word=p.word;
        }
        catch(Exception e){}//tod
        serverMsg(String.format("client %s with %s guessed %c",ip.toString(),word,c));
        
        try{
            ans=p.guess(c);
        }
        catch(Exception e){throw new RemoteException(e.toString());}
        if(!ans.isEmpty()){
            switch(ans.get(0)){
                case -2:
                    System.out.printf("%s PERDEU! R:%d W:%d\n",ip.toString(),p.scoreR,p.scoreW);
                    break;
                case -3:
                    ans.remove((int)0);
                    System.out.printf("%s ACERTOU! R:%d W:%d\n",ip.toString(),p.scoreR,p.scoreW);
                    break;
            }
        }
        System.out.println("Lives:"+p.lives);

        return ans;
    }
    public int beat()throws RemoteException{
        return 1;
    }
    
    public static void main(String args[]) {
        Registry registry;
        Registry slaveRegistry;
        Server obj= new Server(args);
        try{
            // obj = new Server();
            Hangman stub = (Hangman) UnicastRemoteObject.exportObject(obj, 0);
            HangmanMaster sstub = (HangmanMaster) UnicastRemoteObject.exportObject(obj.master, 0);

            registry = LocateRegistry.createRegistry(4242);
            slaveRegistry = LocateRegistry.createRegistry(4243);

            registry.bind("hangmanServer", stub);
            slaveRegistry.bind("hangmanMaster", sstub);

            System.out.println("Servidor preparado!");
        }catch(Exception e){
            System.err.println("Server exception: " + e.toString());
            System.exit(-1);
        }
        availableCommands();
        Scanner input = new Scanner(System.in);
        System.out.print(">");
        while (input.hasNext()){
            switch(input.nextLine()){
                case "clients":
                    printClientList(obj.clientsId);
                    break;
                case "slaves":
                    printSlavesList(obj.master.slavesId);
                    break;
                case "help":
                    availableCommands();
                    break;

                case "stop":
                    // UnicastRemoteObject.unexportObject(registry);
                    System.exit(0);
                default:

                    System.out.println("Comando invalido");

            }
            System.out.print(">");
        }
    }
}