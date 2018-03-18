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
}