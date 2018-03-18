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

public class Server extends RemoteServer implements Hangman {
    static class ClientStatus{
        ClientStatus(){
            tries=new ArrayList<Character>();
            scoreR=scoreW=uCount=charsR=0;
        }
        ClientStatus setLives(int l){
            lives=l;
            return this;
        }
        ClientStatus setWordUniqCount(String w){
            uCount=charsR=0;
            LinkedList<Character> l=new LinkedList<Character>();
            int s=w.length();
            for(int i=0;i<s;i++){
                if(!l.contains(w.charAt(i))){
                    uCount++;
                }
                l.push(w.charAt(i));
            }
            System.out.println("Ucount="+uCount);
            return this;
        }
        ClientStatus reset(){
            tries.clear();
            uCount=charsR=0;
            return this;
        }
        int lives;
        
        int uCount;
        int charsR;

        int scoreR;
        int scoreW;
        ArrayList<Character> tries;
    }
    SlaveServer slaveServer;
    HashMap<InetAddress,Integer> clientsId;
    HashMap<InetAddress,String> clientsWord;
    HashMap<InetAddress,ClientStatus> clientsStatus;
    String topic;
    int dictLen;
    int minWord;
    int maxWord;
    int noLives;
    
    ArrayList<String> words; //todo make this threadsafe

    Hangman.HangmanInfo HInfo;
    LinkedList<Integer> idPool;//this too
    static public int IDPOOL_SIZE = 128;
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
            if(dictLen==0)throw new Exception("Arquivo vazio");
        }
        catch(Exception e){
            System.err.printf("não foi possivel abrir %s pois %s\n",dictPath,e.toString());
            System.exit(1);
        }
        slaveServer= new SlaveServer(words);
        System.out.printf("no de palavras carregadas:%d\n",dictLen);
        System.out.printf("maior palavra:%d menor palavra:%d\n",maxWord,minWord);
        idPool=new LinkedList<Integer>();
        for(int i=1;i<IDPOOL_SIZE+1;i++){
            idPool.push(i);
        }
        Collections.shuffle(idPool);

        HInfo = new Hangman.HangmanInfo();
        HInfo.topic=topic;
        HInfo.minWord=minWord;
        HInfo.maxWord=maxWord;
        HInfo.lives=noLives;

        clientsId=new HashMap<InetAddress,Integer>();
        clientsWord=new HashMap<InetAddress,String>();
        clientsStatus=new HashMap<InetAddress,ClientStatus>();
    }
    static void printClientList(HashMap<InetAddress,Integer> l){
        System.out.printf("%-11s %-32s\n","ip","id");
        for(Map.Entry<InetAddress,Integer> e : l.entrySet()){
            System.out.printf("%-11s %-32d\n",
                e.getKey().toString(),
                e.getValue());
        }
    }
    static void availableCommands(){
       System.out.println("Comandos disponiveis");
       System.out.println("clients [mostre lista de clientes]");
       System.out.println("help [mostra essa lista]");
       System.out.println("stop [para o servidor]");
    }
    static private void serverMsg(String msg){
        System.out.printf("\r[SERVIDOR] %s\n>",msg);

    }
    private String choseWord(){
        return words.get(new Random().nextInt(words.size()));
    }
    //client rpcs
    public Hangman.HangmanInfo getHangmanInfo(){
        return HInfo;
    }
    public int connect()throws ServerNotActiveException,UnknownHostException{
        // if(idPool.empty())throw new Exception("Acabou ids"); //TODO
        int id=idPool.pop();
        String ip=getClientHost();
        InetAddress ipAddr=InetAddress.getByName(ip);
        clientsId.put(ipAddr,id);
        clientsStatus.put(ipAddr,new ClientStatus());
        serverMsg(ip+" se conectou");
        return id;
    }
    public int getWord()throws RemoteException{
        String chose =choseWord();//TODO maybe not in this class
        String ip="";
        InetAddress ipAddr=null;
        int ans=chose.length();;
        try {
            ip=getClientHost();
            ipAddr=InetAddress.getByName(ip);
        }
        catch(Exception e){}//todo
        if(clientsWord.get(ipAddr)!=null){
            throw new RemoteException("Palavra já escolhida para cliente");
        }
        serverMsg(String.format("chose %s to %s\n",chose,ip));
        clientsStatus.get(ipAddr).setLives(noLives).setWordUniqCount(chose);
        clientsWord.put(ipAddr,chose);
        
        return ans;
    }
    public ArrayList<Integer> guess(char c)throws RemoteException{
        c=Character.toLowerCase(c);
        ArrayList<Integer> ans=new ArrayList<Integer>();
        String word="";
        InetAddress ip=null;
        try{
            ip=InetAddress.getByName(getClientHost());
            word=clientsWord.get(ip);
        }
        catch(Exception e){}//todo
        serverMsg(String.format("client %s with %s guessed %c",ip.toString(),word,c));
        ClientStatus cs=clientsStatus.get(ip);
        if(cs.uCount==0)throw new RemoteException("Palavra não foi escolhida!");
        if(cs.tries.contains(c)){
            ans.add(-1);
            return ans;
        }
        cs.tries.add(c);
        int s=word.length();
        for(int i=0;i<s;i++){
            if(word.charAt(i)==c){
                ans.add(i);
            }
        }
        if(ans.isEmpty()){
            cs.lives--;
            System.out.println("WRONG");
            if(cs.lives==0){
                cs.scoreW++;
                cs.reset();
                System.out.printf("%s got word wrong! R:%d W:%d",ip.toString(),cs.scoreR,cs.scoreW);
                clientsWord.remove(ip);
                ans.add(-2);
                for(char ch:word.toCharArray()){
                    ans.add((int)ch);
                }
            }
        }
        else if(++cs.charsR==cs.uCount){
            cs.scoreR++;
            cs.reset();
            clientsWord.remove(ip);
            System.out.printf("%s got word right! R:%d W:%d",ip.toString(),cs.scoreR,cs.scoreW);
        }
        System.out.println("Lives:"+cs.lives);
        return ans;
    }
    // int pong()throws RemoteException{
    //     return 1;
    // }
    //
    public static void main(String args[]) {
        Registry registry;
        Registry slaveRegistry;
        Server obj= new Server(args);
        try{
            // obj = new Server();
            Hangman stub = (Hangman) UnicastRemoteObject.exportObject(obj, 0);
            HangmanMaster sstub = (HangmanMaster) UnicastRemoteObject.exportObject(obj.slaveServer, 0);

            registry = LocateRegistry.createRegistry(4242);
            slaveRegistry = LocateRegistry.createRegistry(4243);

            registry.bind("hangmanClient", stub);
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
                case "help":
                    availableCommands();
                    break;
                case "stop":
                    // UnicastRemoteObject.unexportObject(registry);
                    System.exit(1);
                default:

                    System.out.println("Comando invalido");

            }
            System.out.print(">");
        }
    }
}