//scrap
package hangman;

import java.util.ArrayList;
import java.io.RandomAccessFile;
import java.io.File;

import java.io.IOException;
import java.io.FileNotFoundException;
public class WordCacher{
	RandomAccessFile rat;
	int lastE;
	String lastL;
	public WordCacher(String p)throws FileNotFoundException{
		
			rat=new RandomAccessFile(new File(p),"r");
		
		lastE=Integer.MAX_VALUE;
		lastL=null;
	}
	public ArrayList<String> get(int s,int e){
		ArrayList<String> ans=new ArrayList<String>();
		if(s>e)return ans;
		int i;
		if(lastE!=s){
			i=0;
			try{
				rat.seek(0);
			}
			catch(Exception err){
				System.err.println("Erro ao processar dicionário");
				System.exit(-1);
			}
		}
		else{
			if(lastL!=null)ans.add(lastL);
			i=s;
		}
		String l=null;
		try{
			while((l = rat.readLine()) != null){
				if(i>=s){
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
		lastL=l;
		return ans;
	}
}