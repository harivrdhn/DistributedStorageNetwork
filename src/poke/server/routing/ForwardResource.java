/*
 * copyright 2013, gash
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
package poke.server.routing;

import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.BroadcastDecoderPipeline;
import poke.client.BroadcastHandler;
import poke.client.ClientDecoderPipeline;
import poke.client.ClientListener;
import poke.client.ClientPrintListener;
import poke.server.Server;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

import eye.Comm.Document;
import eye.Comm.Header;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;

/**
 * The forward resource is used by the ResourceFactory to send requests to a
 * destination that is not this server.
 * 
 * Strategies used by the Forward can include TTL (max hops), durable tracking,
 * endpoint hiding.
 * 
 * @author gash
 * 
 */
public class ForwardResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("Server BC");
	private String host;
	private int port;
	private ChannelFuture channelBC; // do not use directly call connect()!
	private ClientBootstrap bootstrapBC;
	ClientDecoderPipeline clientPipeline;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorkerBC workerBC;
	private ArrayList<NodeDesc> nextNodeDesc = Server.pathsBC;
	private String nextNode;
	private ServerConf cfg;
	Request fwd = null;
	String[] originatorID = {"zero","one"};
		
	public ForwardResource() 		
	{		
		this.host = nextNodeDesc.get(0).getHost();
		this.port = nextNodeDesc.get(0).getPort();
		init();
	}

	public ServerConf getCfg() {
		return cfg;
	}

	/**
	 * Set the server configuration information used to initialized the server.
	 * 
	 * @param cfg
	 */
	public void setCfg(ServerConf cfg) {
		this.cfg = cfg;
	}
	
	@Override
	public Response process(Response response) {
		System.out.println("in process of Response ------ Forward Resourse");
		return response;
	}

	@Override
	public Response process(Request request) {
//		String nextNode = determineForwardNode(request);
		Response reply = null;
		String currentNode = null;
		
		for(int i=0;i<originatorID.length;i++)
		{
			if(originatorID[i].equals(nextNodeDesc.get(0).getNodeId()))
				currentNode = originatorID[(i+3)%(originatorID.length)];
		}
		
		ResultSet rs = Server.databaseStorage.findDocument(request.getBody().getDoc().getDocName());
		String docname = null;
		Long metadataid = null;
		String nodename = null;
		long totalChunks = 0;
		boolean resultFound = false;
		
		try {
			while(rs.next()){
				resultFound = true;
				docname = rs.getString("filename");
				totalChunks = (long)rs.getInt("numberofchunks");
				//nodename = rs.getString("nodename");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("error retrieving file ");
		}
		
		if(resultFound)
		{
			if(request.getHeader().getTag().equalsIgnoreCase("DOCREMOVE")){
                boolean executed = Server.databaseStorage.removeDocument(request);      
                Response.Builder rb = Response.newBuilder();                
                PayloadReply.Builder pb = PayloadReply.newBuilder();                                             
                Document.Builder docs = Document.newBuilder();
                docs.setDocName(docname);
                docs.setTotalChunk(totalChunks);
                pb.addDocs(docs);
               
                Header.Builder h = Header.newBuilder();
                h.setOriginator(currentNode);     // change to node id
                h.setTag(request.getHeader().getTag());
                h.setTime(System.currentTimeMillis());
                h.setRoutingId(request.getHeader().getRoutingId());
                
                if(executed){
                	h.setReplyMsg("FILE "+docname+" DELETED");
                	h.setReplyCode(ReplyStatus.SUCCESS);}
                else{
                	h.setReplyMsg("FILE "+docname+" NOT DELETED");
                	h.setReplyCode(ReplyStatus.FAILURE);}
                rb.setHeader(h);
                rb.setBody(pb);
                reply = rb.build();
                
            } else{
            	System.out.println("Doc Find /retrieve");
            	int chunkID = 0;
                totalChunks = 0;
                byte[] chunkValue = null;
                   
                    rs = Server.databaseStorage.findDocument(request.getBody().getDoc().getDocName(), true, (int)request.getBody().getDoc().getChunkId());
                    try {
                        while(rs.next()){                       
                            docname = rs.getString("filename");
                            chunkID = (int) rs.getInt("chunkid");
                            totalChunks = rs.getInt("totalchunks");
                            chunkValue = rs.getBytes("chunkvalue");
                            System.out.println("chunk size  "+chunkValue.length);
                        }
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                    System.out.println("error retrieving file "+e);
                    }
                    ByteString chunkContent = ByteString.copyFrom(chunkValue);
                    
                    Response.Builder rb = Response.newBuilder();
                    
                    PayloadReply.Builder pb = PayloadReply.newBuilder();
                   
                    Document.Builder db = Document.newBuilder();
                    db.setChunkContent(chunkContent);
                    db.setDocName(docname);
                    db.setChunkId(chunkID);
                    db.setTotalChunk(totalChunks);
                  
                    NameSpace.Builder ns = NameSpace.newBuilder();
                    ns.setName(request.getBody().getSpace().getName());
                   
                    pb.addSpaces(ns);                   
                    pb.addDocs(db);
                    pb.setStats(db);
                    rb.setBody(pb.build());
                   
                    Header.Builder h = Header.newBuilder();
                    h.setOriginator(currentNode);     // change to node id
                    h.setTag(request.getHeader().getTag());
                    h.setTime(System.currentTimeMillis());
                    h.setRoutingId(request.getHeader().getRoutingId());
                    h.setReplyCode(ReplyStatus.SUCCESS);
                    h.setReplyMsg(" FILE "+ docname +" FOUND");           
                    rb.setHeader(h);
                    reply = rb.build();
            }
			
                return reply;
			
		}
		else{		
			if(request.getHeader().hasRemainingHopCount())
			{
				if(request.getHeader().getRemainingHopCount() > 1)
				{
					nextNode = nextNodeDesc.get(0).getNodeId();					
					System.out.println("********* The Next Node is  : " + nextNode);
					System.out.println("********* The currentNode is  : " + currentNode);
					if (nextNode != null) {
						
						fwd = ResourceUtil.buildForwardMessage(request, currentNode, nextNode);
						/*MonitorHandler hand = new MonitorHandler();
						hand.send(fwd);*/		
						
						try {
							// enqueue message
							outbound.put(fwd);
						} catch (InterruptedException e) {
							logger.warn("Unable to deliver message, queuing");
						}
						
						// We can also accept connections from a other ports (e.g., isolate read
						// and writes)
						System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ BroadCast public network");
						System.out.println("Starting Broad Cast server, listening on port = " + port);					
					}
				}
				else {
						// cannot forward the message - no edge or already traveled known
						// edges
	
						// TODO should we just fail silently?
	
						Response.Builder rb = Response.newBuilder();
						PayloadReply.Builder pb = PayloadReply.newBuilder();
						rb.setBody(pb.build());
						Header.Builder h = Header.newBuilder();
						h.setOriginator(currentNode); 	// change to node id
						h.setTag(request.getHeader().getTag());
						h.setTime(System.currentTimeMillis());
						h.setRoutingId(request.getHeader().getRoutingId());
						h.setReplyCode(ReplyStatus.FAILURE);
						h.setReplyMsg("404 FILE NOT FOUND");
						
						rb.setHeader(h);
						reply = rb.build();
						
						return reply;
					}
				}
			
			else
			{
				nextNode = nextNodeDesc.get(0).getNodeId();
				System.out.println("********* The Next Node is  : " + nextNode);
				System.out.println("********* The currentNode is  : " + currentNode);
				if (nextNode != null) {
					
					fwd = ResourceUtil.buildForwardMessage(request,currentNode, nextNode);
					/*MonitorHandler hand = new MonitorHandler();
					hand.send(fwd);*/		
					
					try {
						// enqueue message
						outbound.put(fwd);
					} catch (InterruptedException e) {
						logger.warn("Unable to deliver message, queuing");
					}
					
					// We can also accept connections from a other ports (e.g., isolate read
					// and writes)
					System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ BroadCast public network");
					System.out.println("Starting Broad Cast server, listening on port = " + port);	
				}
			}
		}
		 
		return null;
	}
	private void init()
	{
		//Sandeep
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
		bootstrapBC = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
		

		// Set up the pipeline factory.
		BroadcastDecoderPipeline broadcastPipeline = new BroadcastDecoderPipeline();
		bootstrapBC.setPipelineFactory(broadcastPipeline);
		ClientListener listenerBC = new ClientPrintListener("BroadcastListener");
		broadcastPipeline.addListener(listenerBC);


		// tweak for performance
		bootstrapBC.setOption("connectTimeoutMillis", 10000);
		bootstrapBC.setOption("tcpNoDelay", true);
		bootstrapBC.setOption("keepAlive", true);
		
		// start outbound message processor
		workerBC = new OutboundWorkerBC(this);
		workerBC.start();
	}
		
	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channelBC == null) {
			System.out.println("---> connecting");
			channelBC = bootstrapBC.connect(new InetSocketAddress(host, port));

			// cleanup on lost connection

		}

		// wait for the connection to establish
		channelBC.awaitUninterruptibly();

		if (channelBC.isDone() && channelBC.isSuccess())
			return channelBC.getChannel();
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
	protected class OutboundWorkerBC extends Thread {
		ForwardResource connBC;
		boolean forever = true;

		public OutboundWorkerBC(ForwardResource connBC) {
			this.connBC = connBC;

			if (connBC.outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel ch = connBC.connect();
			//ChannelFuture cf = ch.write(fwd);
			
			/*if (cf.isDone() && !cf.isSuccess()) {
				logger.error("**************failed to broadcast message!");
			}*/
			if (ch == null || !ch.isOpen()) {
				ForwardResource.logger.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && connBC.outbound.size() == 0)
				{
					System.out.println("%%%%%%%%%%%%%  in OutboundWorker break loop %%%%%%%%%");
					break;
				}
				try {
					// block until a message is enqueued
					GeneratedMessage msg = connBC.outbound.take();
					if (ch.isWritable()) {
						BroadcastHandler handler = connBC.connect().getPipeline().get(BroadcastHandler.class);

						if (!handler.send(msg))
							connBC.outbound.putFirst(msg);

					} else
						connBC.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					ForwardResource.logger.error("Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				ForwardResource.logger.info("connection queue closing");
			}
		}
	}

	/**
	 * Find the nearest node that has not received the request.
	 * 
	 * TODO this should use the heartbeat to determine which node is active in //should be done later
	 * its list.
	 * 
	 * @param request
	 * @return
	 */
	private String determineForwardNode(Request request) {
		List<RoutingPath> paths = request.getHeader().getPathList();
		if (paths == null || paths.size() == 0) {
			// pick first nearest
			NodeDesc nd = cfg.getNearest().getNearestNodes().values().iterator().next();
			return nd.getNodeId();
		} else {
			// if this server has already seen this message return null
			for (RoutingPath rp : paths) {
				for (NodeDesc nd : cfg.getNearest().getNearestNodes().values()) {
					if (!nd.getNodeId().equalsIgnoreCase(rp.getNode()))
						return nd.getNodeId();
				}
			}
		}

		return null;
	}
}
