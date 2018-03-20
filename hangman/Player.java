package hangman;

import java.util.ArrayList;
import java.util.LinkedList;
public class Player{
    int lives;
    
    int uCount;
    int charsR;

    int scoreR;
    int scoreW;
    ArrayList<Character> tries;

    String word;

    Player(){
        tries=new ArrayList<Character>();
        scoreR=scoreW=uCount=charsR=0;
        word="";
    }
    Player setLives(int l){
        lives=l;
        return this;
    }
    Player setWordUniqCount(String w){
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
    Player setWord(String w){
        setWordUniqCount(w);
        word=w;
        return this;
    }
    Player reset(){
        tries.clear();
        uCount=charsR=0;
        word="";
        return this;
    }
    ArrayList<Integer> guess(char c)throws Exception{
        ArrayList<Integer> ans=new ArrayList<Integer>();
        if(uCount==0)throw new Exception("Palavra não foi escolhida!");
        if(tries.contains(c)){
            ans.add(-1);
            return ans;
        }
        tries.add(c);
        int s=word.length();
        for(int i=0;i<s;i++){
            if(word.charAt(i)==c){
                ans.add(i);
            }
        }
        if(ans.isEmpty()){
            if(--lives==0){
                scoreW++;
                ans.add(-2);
                for(char ch:word.toCharArray()){
                    ans.add((int)ch);
                }
                reset();
            }
        }
        else if(++charsR==uCount){
            ans.add(0,-3);
            scoreR++;
            reset();
        }
        return ans;
    }
}   