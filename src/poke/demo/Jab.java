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
package poke.demo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import poke.client.ClientConnection;
import poke.client.ClientListener;
import poke.client.ClientPrintListener;

public class Jab {
	private String tag;
	private int count;

	public Jab(String tag) {
		this.tag = tag;
	}

	public void run() throws IOException {
	//public void run(FileInputStream input) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		
		for (int i = 0; i < 10; i++) {
			count++;
			cc.poke(tag, count);
		}
	}
	
	public void runNew(FileInputStream input) throws IOException {
	//public void run(FileInputStream input) throws IOException {
		ClientConnection cc = ClientConnection.initConnection("localhost", 5570);
		ClientListener listener = new ClientPrintListener("jab demo");
		cc.addListener(listener);
		cc.fileTransfer(input);
		
	}

	public static void main(String[] args) {
		try {
			Jab jab = new Jab("jab");
			jab.run();
			//FileInputStream input = new FileInputStream("/home/padmaja/cmpe275/input.txt");
			//jab.runNew(input);
			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			Thread.sleep(500000);
			System.out.println("\nwoke up");
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
