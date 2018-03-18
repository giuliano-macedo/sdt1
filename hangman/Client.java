package hangman;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Scanner;
import java.util.ArrayList;

import java.lang.StringBuilder;

public class Client {
    // static class beatRunnable implements Runnable{
    //     Hangman server;
    //     public beatRunnable(Hangman sv){
    //         server=sv;
    //     }
    //     public void run(){
    //         while(true){
    //             try{server.pong();}
    //             catch(Exception e){
    //                 err("Conexão com servidor perdida");
    //             }
    //             try{Thread.sleep(500);}
    //             catch(Exception e){}
    //         }
    //     }
    // }
    private Client() {}
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
            System.out.printf("Connected with:%s with id:%s\n",host,ans.id);
            System.out.printf("topic: %s min:%d max:%d nolives:%d\n",ans.hi.topic,ans.hi.minWord,ans.hi.maxWord,ans.hi.lives);
        
        }catch(Exception e){
            throw e;
        }
        return ans;
    }

    public static void main(String[] args){
        ServerInfo testsi=new ServerInfo();
        try {testsi=connectTo("127.0.0.1");}
        catch(Exception e){err(e.toString());}
        // Runnable r=new beatRunnable(testsi.server);
        // new Thread(r).start();
        Scanner input = new Scanner(System.in);
        int wordLen=0;
        try{wordLen=testsi.server.getWord();}
        catch(Exception e){err(e.toString());}
        ArrayList<Character> word=new ArrayList<Character>(wordLen); //FIX INDEXOUTBOUND ERR
        for(int i=0;i<wordLen;i++){word.add('\0');}
        while(word.contains('\0')){
            System.out.printf("Vidas:%d\n",testsi.hi.lives);
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
            ArrayList<Integer> b=new ArrayList<Integer>();
            try{b=testsi.server.guess(u);}
            catch(Exception e){err(e.toString());}
            if(b.isEmpty()){
                System.out.println("ERROU!");
                testsi.hi.lives--;
                continue;
            }
            if(b.get(0)==-2){
                System.out.print("PERDEU! a palavra era ");
                int s=b.size();
                for(int i=1;i<s;i++){
                    System.out.print((char)b.get(i).intValue());
                }
                System.out.print('\n');
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
            System.out.printf("ACERTOU! a palavra era %s\n",word);
        }

    }
}