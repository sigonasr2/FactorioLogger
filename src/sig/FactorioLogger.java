package sig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import sig.utils.FileUtils;

public class FactorioLogger {
	public static void main(String[] args) {
		String webhook_url = FileUtils.readFromFile("webhook.txt")[0];
		int playersOnline=0;
		List<String> onlineUsers = new ArrayList<>();
		
		FileReader file;
		try {
			file = new FileReader("factorio.log");
			BufferedReader reader = new BufferedReader(file);
			while (true) {
				String line = reader.readLine();
				if (line!=null) {
					boolean isValidMessage = false;
					int lastChar = 0;
					int countStage=0;

					check:
					for (int i=0;i<line.length();i++) {
						if (line.charAt(i)=='.') {
							break check;
						}
						switch (countStage) {
							case 0:
							case 1:{
								if (line.charAt(i)=='-') {
									countStage++;
								}
							}break;
							case 2:{
								if (line.charAt(i)==' ') {
									countStage++;
								}
							}break;
							case 3:
							case 4:{
								if (line.charAt(i)==':') {
									countStage++;
								}
							}break;
							case 5:{
								if (line.charAt(i)==' ') {
									isValidMessage=true;
									lastChar=i+1;
									break check;
								}
							}break;
						}
					}
					if (isValidMessage) {
						String message = line.substring(lastChar);
						if (message.contains("[JOIN]")) {
							for (int j=0;j<onlineUsers.size();j++) {
								if (onlineUsers.get(j).equalsIgnoreCase(message.replace(" joined the game","").replace("[JOIN] ",""))) {
									onlineUsers.remove(j);
									break;
								}
							}
							onlineUsers.add(message.replace(" joined the game","").replace("[JOIN] ",""));
							message=message+" **("+onlineUsers.size()+" player"+(onlineUsers.size()!=1?"s":"")+" online)**";
						}
						if (message.contains("[LEAVE]")) {
							for (int j=0;j<onlineUsers.size();j++) {
								if (onlineUsers.get(j).equalsIgnoreCase(message.replace(" left the game","").replace("[LEAVE] ",""))) {
									onlineUsers.remove(j);
									break;
								}
							}
							message=message+" **("+onlineUsers.size()+" player"+(onlineUsers.size()!=1?"s":"")+" online)**";
						}
						System.out.println(message);
			 			//PostMessage(webhook_url,message);
					}
				} else {
					Thread.sleep(1000);
				}
			}
		} catch (IOException | InterruptedException e1) {
			e1.printStackTrace();
		}
		
		
		
	}

	private static void PostMessage(String webhook_url,String message) {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(webhook_url);
		
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("content",message));
		post.setEntity(new UrlEncodedFormEntity(params));
		CloseableHttpResponse resp = null;
		try {
			resp = client.execute(post);
			HttpEntity ent = resp.getEntity();
			if (ent!=null) {
			    try (InputStream instream = ent.getContent()) {
			    	Scanner s = new Scanner(instream).useDelimiter("\\A");
			    	String result = s.hasNext() ? s.next() : "";
			    	System.out.println(result);
			    	instream.close();
			    } catch (UnsupportedOperationException | IOException e) {	
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
