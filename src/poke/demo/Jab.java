/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

// CODE CHANGES BY PADMAJA

package poke.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import poke.client.ClientConnection;
import poke.client.ClientListener;
import poke.client.ClientPrintListener;

public class Jab {
	private String tag;
	public static int num=0;//leader
	boolean flag;//leader
	private int count;

	public Jab(String tag) {
		this.tag = tag;
	}

	public void run() throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		
		for (int i = 0; i < 1; i++) {
			count++;
			cc.poke(tag, count);
		}
	}
	
	public void runDocumentAdd(String docPath) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		cc.poke(tag+"|lead", 0);
		flag=true;
		while(flag){
			if(num==1){
				cc.fileTransfer(docPath,tag);
				flag=false;
				System.out.println("File action complete. Sending Lead ack.");
				cc.poke(tag+"|lead", 2);
			}
		}
	}
	public void runDocumentFind(String docPath) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		cc.poke(tag+"|lead", 0);
		flag=true;
		while(flag){
			if(num==1){
				cc.fileFind(docPath, tag);
				flag=false;
				cc.poke(tag+"|lead", 2);
			}
		}
	}
	public void runDocumentRemove(String docPath) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		cc.poke(tag+"|lead", 0);
		flag=true;
		while(flag){
			if(num==1){
				cc.fileRemove(docPath, tag);
				flag=false;
				cc.poke(tag+"|lead", 2);
			}
		}
	}

	public static void main(String[] args) {
		try {
			Jab jab = new Jab(args[0]);
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			if(jab.tag.equalsIgnoreCase("DOCADD")){
				System.out.println("enter file path to add file");
				String docPath = br.readLine();				
				jab.runDocumentAdd(docPath);			
			}
			else if (jab.tag.equalsIgnoreCase("DOCFIND")){
				System.out.println("enter file name to search");
				String docName = br.readLine();
				jab.runDocumentFind(docName);
			}
			else if(jab.tag.equalsIgnoreCase("DOCREMOVE")){
				System.out.println("enter file name to REMOVE");
				String docPath = br.readLine();
				jab.runDocumentRemove(docPath);
			}
			else{
				jab.run();
				System.out.println("the file operation is not appropriate! please try again as DOCADD,DOCFIND,DOCREM,DOCEDIT");
			}
			
			
			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			Thread.sleep(5000000);
			System.out.println("\nwoke up");
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}