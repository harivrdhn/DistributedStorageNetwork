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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import poke.client.ClientConnection;
import poke.client.ClientListener;
import poke.client.ClientPrintListener;

public class JabClientServer1 {
	private String tag;
	private int count;

	public JabClientServer1(String tag) {
		this.tag = tag;
	}

	public void run() throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		
		for (int i = 0; i < 3; i++) {
			count++;
			cc.poke(tag, count);
		}
	}
	
	public void runDocumentAdd(File input) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5573);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		//cc.fileTransfer(input,tag);
	}
	public void runDocumentFind(String docName) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5573);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		cc.fileFind(docName, tag);
	}

	public static void main(String[] args) {
		try {
			JabClientServer1 jab = new JabClientServer1(args[0]);
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			/*if(jab.tag.equalsIgnoreCase("DOCADD")){
				System.out.println("enter file path to add file");
				//br.re
				//File file = new File(args[1]); 
			
			}*/
			if (jab.tag.equalsIgnoreCase("DOCFIND")){
				System.out.println("enter file name to search");
				String docName = br.readLine();
				jab.runDocumentFind(docName);
			}
		/*	else{
				System.out.println("the file operation is not appropriate! please try again as DOCADD,DOCFIND,DOCREM,DOCEDIT");
			}*/
			//jab.runDocumentFind(file);
			//jab.run();
			//file.getName();
			//FileInputStream input = new FileInputStream("/home/padmaja/cmpe275/input.txt");
			
			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			Thread.sleep(500000000);
			System.out.println("\nwoke up");
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
