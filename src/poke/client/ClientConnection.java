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

package poke.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.NameSpace;
import eye.Comm.Payload;
import eye.Comm.Request;

/**
 * provides an abstraction of the communication to the remote server.
 * 
 * @author gash
 * 
 */
public class ClientConnection {
	protected static Logger logger = LoggerFactory.getLogger("client");
	static ArrayList<byte[]> chunkArray = new ArrayList<byte[]>();

	private String host;
	private int port;
	private ChannelFuture channel; // do not use directly call connect()!
	private ClientBootstrap bootstrap;
	ClientDecoderPipeline clientPipeline;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker worker;

	protected ClientConnection(String host, int port) {
		this.host = host;
		this.port = port;

		init();
	}

	/**
	 * release all resources
	 */
	public void release() {
		bootstrap.releaseExternalResources();
	}

	public static ClientConnection initConnection(String host, int port) {

		ClientConnection rtn = new ClientConnection(host, port);
		return rtn;
	}

	/**
	 * add an application-level listener to receive messages from the server (as
	 * in replies to requests).
	 * 
	 * @param listener
	 */
	public void addListener(ClientListener listener) {
		try {
			if (clientPipeline != null)
				clientPipeline.addListener(listener);
		} catch (Exception e) {
			logger.error("failed to add listener", e);
		}
	}

	public void poke(String tag, int num) {
		// data to send
		Finger.Builder f = eye.Comm.Finger.newBuilder();
		f.setTag(tag);
		f.setNumber(num);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setFinger(f.build());
		r.setBody(p.build());
		
		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag("test finger");
		h.setTime(System.currentTimeMillis());
		h.setRoutingId(eye.Comm.Header.Routing.FINGER);
		r.setHeader(h.build());

		eye.Comm.Request req = r.build();

		try {
			// enqueue message
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	
public void fileTransfer(String inputFilePath,String tag) {
		
		//  File Chunk
		InputStream fileReader = null;
		int chunk_size = 1024;
		byte[] chunk = null;
		int totalBytesRead = 0;
		int chunkID = 0;
		int bytesRead =0;
		File inputFile  = null;		 
		int id = 0;	
		String nameSpace = "";
		id = inputFilePath.lastIndexOf('/');
		if(id == -1)
			id = inputFilePath.lastIndexOf('\\');			
		
		if(id == -1 || id == 0 )
			nameSpace = "/home/sandeep/Documents/275";
		else	
			nameSpace = inputFilePath.substring(0,id);
		
		String fileName = inputFilePath.substring(id+1, inputFilePath.length());
		System.out.println("NAMESPACE = "+ nameSpace + "  FILENAME = "+fileName);
		
		String filePath = nameSpace+"/"+fileName;
		while(true){
			try{
				inputFile = new File(filePath);
			
			fileReader = new BufferedInputStream( new FileInputStream(inputFile));
			} catch (FileNotFoundException FNF) {
				logger.warn(" -- FILE NOT FOUND EXCEPTIOn");break;
			}
			 int file_size = (int) inputFile.length();
			 int number_of_chunks = 0;
			 
			 System.out.println("input file length  "+inputFile.length());
			 
			 for(int i=4;;i++){
				 chunk_size = file_size/i;
				 if(chunk_size< 64*1024*700){
					 number_of_chunks = i+1;
					 break;
				 }				 
			 }
			  
			 while ( totalBytesRead < file_size ){			    
			     int bytesRemaining = file_size-totalBytesRead;
			     if ( bytesRemaining < chunk_size )
			    	 chunk_size = bytesRemaining;			      
			     chunk = new byte[chunk_size]; //Temporary Byte Array
			    try{
			    	
				    bytesRead = fileReader.read(chunk, 0, chunk_size);
				     
				     if ( bytesRead > 0) // If bytes read is not empty
				     {
				      totalBytesRead += bytesRead;
				      chunkID++;
				     }
			    } catch (IOException e) {
					logger.error("UNABLE TO READ FILE");
			    }
			    
			    System.out.println("BYTES  "+chunk);
			    System.out.println("***********************************   chunk size  "+chunk_size +  "   "+ bytesRead );
			    System.out.println("**************     CHUNKID  "+chunkID + "  No of chunks  "+ number_of_chunks );
			     
				// data to send
				Finger.Builder f = eye.Comm.Finger.newBuilder();
				f.setTag(tag);
		
				Document.Builder doc = eye.Comm.Document.newBuilder();				
				
				doc.setChunkContent(ByteString.copyFrom(chunk));
				doc.setChunkId(chunkID);
				doc.setDocName(fileName);
				doc.setDocSize(file_size);
				doc.setTotalChunk(number_of_chunks);
				
				// payload containing data
				Request.Builder r = Request.newBuilder();
				eye.Comm.Payload.Builder p = Payload.newBuilder();
				
				NameSpace.Builder ns = NameSpace.newBuilder();
				ns.setName(nameSpace);
				p.setSpace(ns);
				p.setDoc(doc);
				r.setBody(p.build());
				
				// header with routing info
				eye.Comm.Header.Builder h = Header.newBuilder();
				h.setOriginator("zero");
				h.setTag(tag);
				h.setTime(System.currentTimeMillis());
				h.setRoutingId(eye.Comm.Header.Routing.DOCADD);
				r.setHeader(h.build());

				eye.Comm.Request req = r.build();

				try {
					// enqueue message
					outbound.put(req);
				} catch (InterruptedException e) {
					logger.warn("Unable to deliver message, queuing");
				}
			}	
			 if(chunkID == number_of_chunks)
				 break;
		}
	}
	
	public void fileFind(String docPath,String tag) {
		// data to send
		String nameSpace = "";
		int id = docPath.lastIndexOf('/');
		if(id == -1)
			id = docPath.lastIndexOf('\\');			
		
		if(id == -1 || id == 0 )
			nameSpace = "/home/sandeep/Documents/275";
		else	
			nameSpace = docPath.substring(0,id);
		
		String fileName = docPath.substring(id+1, docPath.length());
		System.out.println("NAMESPACE = "+ nameSpace + "  FILENAME = "+fileName);
		
		docPath = nameSpace+"/"+fileName;
		
		Finger.Builder f = eye.Comm.Finger.newBuilder();
		f.setTag(tag);

		Document.Builder doc = eye.Comm.Document.newBuilder();
		doc.setDocName(fileName);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		
		NameSpace.Builder ns = NameSpace.newBuilder();
		ns.setName(nameSpace);
		p.setSpace(ns);
		
		p.setDoc(doc);
		r.setBody(p.build());
		
		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("zero");
		h.setTag(tag);
		h.setTime(System.currentTimeMillis());
		h.setRoutingId(eye.Comm.Header.Routing.DOCFIND);
		r.setHeader(h.build());

		eye.Comm.Request req = r.build();

		try {
			// enqueue message
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
	}
	}
	
	public void fileRemove(String docPath,String tag) {
		// data to send
		String nameSpace = "";
		int id = docPath.lastIndexOf('/');
		if(id == -1)
			id = docPath.lastIndexOf('\\');			
		
		if(id == -1 || id == 0 )
			nameSpace = "/home/sandeep/Documents/275";
		else	
			nameSpace = docPath.substring(0,id);
		
		String fileName = docPath.substring(id+1, docPath.length());
		System.out.println("NAMESPACE = "+ nameSpace + "  FILENAME = "+fileName);
		
		docPath = nameSpace+"/"+fileName;
						
		Finger.Builder f = eye.Comm.Finger.newBuilder();
		f.setTag(tag);

		Document.Builder doc = eye.Comm.Document.newBuilder();
		doc.setDocName(fileName);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
				
		NameSpace.Builder ns = NameSpace.newBuilder();
		ns.setName(nameSpace);
		p.setSpace(ns);
		p.setDoc(doc);
		r.setBody(p.build());
		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("zero");
		h.setTag(tag);
		h.setTime(System.currentTimeMillis());
		h.setRoutingId(eye.Comm.Header.Routing.DOCREMOVE);
		r.setHeader(h.build());

		eye.Comm.Request req = r.build();

		try {
			// enqueue message
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	
	private void init() {
		// the queue to support client-side surging
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		// Set up the pipeline factory.
		clientPipeline = new ClientDecoderPipeline();
		bootstrap.setPipelineFactory(clientPipeline);

		// start outbound message processor
		worker = new OutboundWorker(this);
		worker.start();
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			System.out.println("---> CHANNEL connecting");
			channel = bootstrap.connect(new InetSocketAddress(host, port));

			// cleanup on lost connection

		}

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess())
			return channel.getChannel();
		else
			throw new RuntimeException("Not able to establish connection to server");
	}

	/**
	 * queues outgoing messages - this provides surge protection if the client
	 * creates large numbers of messages.
	 * 
	 * @author gash
	 * 
	 */
	protected class OutboundWorker extends Thread {
		ClientConnection conn;
		boolean forever = true;

		public OutboundWorker(ClientConnection conn) {
			this.conn = conn;

			if (conn.outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel ch = conn.connect();
			if (ch == null || !ch.isOpen()) {
				ClientConnection.logger.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				System.out.println("&&&&&&&&&&  IN BLOCKED WAITING WHILE &&&&&& ");
				if (!forever && conn.outbound.size() == 0)
				{
					System.out.println("%%%%%%%%%%%%%  in OutboundWorker break loop %%%%%%%%%");
					break;
				}
				try {
					// block until a message is enqueued
					GeneratedMessage msg = conn.outbound.take();
					System.out.println("&&&&&&&& INBOUND MESSAGE AFTER BLOCKED WAITING ");
					if (ch.isWritable()) {
						System.out.println("&&&&&&& Channel writable");
						ClientHandler handler = conn.connect().getPipeline().get(ClientHandler.class);

						if (!handler.send(msg)){
							System.out.println(" &&&&& NOT ABLE TO SEND MESSAGE HABDLER");
							conn.outbound.putFirst(msg);
						}

					} else{
						System.out.println("&&&&&&&&& Non writable channel ");
						conn.outbound.putFirst(msg);
					}
						
					System.out.println("&&&&&&&&&&  OUT OF BLOCKED WAITING WHILE &&&&&& ");
				} catch (InterruptedException ie) {
					System.out.println(" &&&&&&&& INTERRUPTED EXCEPTION AT MEASSAGE ENQUUE -- clientConnection.jaava");
					break;
				} catch (Exception e) {
					ClientConnection.logger.error("Unexpected communcation failure", e);
					break;
				}
			}
			System.out.println("&&&&&&&&&&  EXIT BLOCKED WAITING WHILE &&&&&& ");
			if (!forever) {
				ClientConnection.logger.info("connection queue closing");
			}
		}
	}
}
