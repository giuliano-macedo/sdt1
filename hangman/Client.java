package hangman;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

import java.rmi.server.UnicastRemoteObject;

import java.util.Scanner;
import java.util.ArrayList;

import java.lang.StringBuilder;

public class Client extends RemoteServer implements HangmanClient{
    static class HeartBeat implements Runnable{
        Hangman server;
        public HeartBeat(Hangman sv){
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
    ArrayList<String> serversIp;
    public Client() {
        serversIp=new ArrayList<String>();
    }
    static void err(String msg){
        System.err.println(msg);
        System.exit(-1);
    }
    static class ServerInfo{
        Registry registry;
        Hangman server;
        int id;
        Hangman.HangmanInfo hi;
    }
    static ServerInfo connectTo(String host)throws Exception{
        ServerInfo ans=new ServerInfo();
        try{
            ans.registry = LocateRegistry.getRegistry(host,4242);
            ans.server = (Hangman) ans.registry.lookup("hangmanServer");
            ans.hi = ans.server.getHangmanInfo();
            ans.id = ans.server.connect();
            System.out.printf("Conectado com:%s com id:%s\n",host,ans.id);
            System.out.printf("topico: %s minimo:%d maximo:%d no de vidas:%d\n",ans.hi.topic,ans.hi.minWord,ans.hi.maxWord,ans.hi.lives);
        
        }catch(Exception e){
            throw e;
        }
        return ans;
    }

    //rpcs
    public void sendHInfo(Hangman.HangmanInfo hinfo){
        String ip=null;
         try {
            ip=getClientHost().toString().replace("/","");
        }
        catch(Exception e){
            System.err.println("Falha ao obter endereço ip de um servidor");
            return;
        }
        if(!serversIp.contains(ip)){
            System.out.printf("[%-2d] %-10s %-3d %-3d %-3d\n",serversIp.size(),hinfo.topic,hinfo.minWord,hinfo.maxWord,hinfo.lives);
            serversIp.add(ip);
        }
    }
    public int beat(){
        return 1;
    }
    public void exit(){
        err("Ocorreu um erro fatal no servidor");
    }
    //

    public static void main(String[] args){
        Registry registry;
        Client obj= new Client();
        try{
            HangmanClient stub = (HangmanClient) UnicastRemoteObject.exportObject(obj, 0);
            registry = LocateRegistry.createRegistry(4245);
            registry.bind("hangmanClient", stub);
        }catch(Exception e){
            System.err.println("Falha ao iniciar servidor: " + e.toString());
            System.exit(-1);
        }
        try{
           new DatagramSocket().send(new DatagramPacket(new byte[0], 0, InetAddress.getByName("255.255.255.255"), 4246));
        }
        catch(Exception e){
            System.out.println("falha ao discobrir rede "+e.toString());
            return;
        }
        System.out.printf("%-4s %-10s %-3s %-3s %-3s\n","[#]","Tópico","min","max","no de vidas");
        try{
            Thread.sleep(2500);
        }catch(Exception e){}
        Scanner input = new Scanner(System.in);

        System.out.printf("Selecione um dos servidores [0-%d]\n",obj.serversIp.size()-1);
        int usern=Integer.parseInt(input.nextLine());
        if(usern<0||usern>=obj.serversIp.size()){
            System.err.println("Número incorreto");
            System.exit(-1);
        }

        ServerInfo si=new ServerInfo();
        try {si=connectTo(obj.serversIp.get(usern));}
        catch(Exception e){err(e.toString());}
        Runnable r=new HeartBeat(si.server);
        new Thread(r).start();
        

        int wordLen=0;
        ArrayList<Integer> b=null;
        ArrayList<Integer> score=null;
        char g='s';
        while(g=='s'){
            try{wordLen=si.server.getWord();}
            catch(Exception e){err("servidor getWord :"+e.toString());}
            ArrayList<Character> word=new ArrayList<Character>(wordLen); //FIX INDEXOUTBOUND ERR
            for(int i=0;i<wordLen;i++){word.add('\0');}
            while(word.contains('\0')){
                System.out.printf("Vidas:%d\n",si.hi.lives);
                for(int i=0;i<wordLen;i++){
                    if(word.get(i)!='\0'){
                        System.out.print(word.get(i));
                    }
                    else{
                        System.out.print("_ ");
                    }
                }
                System.out.println();
                char u=input.next().charAt(0);
                try{b=si.server.guess(u);}
                catch(Exception e){err("servidor guess:"+e.toString());}
                if(b.isEmpty()){
                    System.out.println("ERROU!");
                    si.hi.lives--;
                    continue;
                }
                if(b.get(0)==-2){
                    String str="";
                    for(Integer i:b.subList(1,b.size()))str+=(char)(int)i;
                    System.out.printf("PERDEU! a palavra era %s\n",str);
                    
                    break;
                }
                if(b.get(0)==-1){
                    System.out.println("Esse caractere já foi escolhido!");
                    continue;
                }
                for(int i=0;i<b.size();i++){
                    word.set(b.get(i),u);
                }
            }
            if(!word.contains('\0')){
                String str="";
                for(Character c:word) str+=c;
                System.out.printf("ACERTOU! a palavra era %s\n",str);
            }
            try{
                score=si.server.getScore();
            }
            catch(Exception e){err("servidor getScore:"+e.toString());}
            System.out.println("Jogar novamente? : [S]im [N]ão");
            System.out.printf("Pontuação : certo:%d errado:%d\n",score.get(0),score.get(1));
            g=Character.toLowerCase(input.next().charAt(0));
        }
    System.exit(1);
    }
}