package hangman;

import java.util.ArrayList;
import java.io.BufferedReader;

import java.io.IOException;
public class WordCacher{
	BufferedReader br;
	int lastE;
	public WordCacher(BufferedReader b){
		br=b;
		lastE=Integer.MAX_VALUE;
	}
	public ArrayList<String> get(int s,int e){
		ArrayList<String> ans=new ArrayList<String>();
		if(s>e)return ans;
		int i;
		if(lastE!=s){
			i=0;
			br.mark(0);
		}
		else{
			i=s;
		}
		String l;
		while((l = br.readLine()) != null){
			if(s>=i){
				if(i>=e){
					break;
				}
				ans.add(l);
			}
			i++;
		}
		lastE=e;
		return ans;
	}
}