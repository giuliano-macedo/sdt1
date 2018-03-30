//scrap
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
		System.out.print("\r cacher "+s+" "+e+"\n>");
		ArrayList<String> ans=new ArrayList<String>();
		if(s>e)return ans;
		int i;
		if(lastE!=s){
			i=0;
			try{
				br.mark(0);
			}
			catch(Exception err){
				System.err.println("Erro ao processar arquivo");
				System.exit(-1);
			}
		}
		else{
			i=s;
		}
		String l;
		try{
			while((l = br.readLine()) != null){
				if(s>=i){
					if(i>=e){
						break;
					}
					ans.add(l);
				}
				i++;
			}
		}
		catch(Exception err){
			System.err.println("Erro ao processar arquivo");
			System.exit(-1);
		}
		lastE=e;
		return ans;
	}
}