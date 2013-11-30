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
package poke.server.queue;

import java.lang.Thread.State;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.Server;
import poke.server.management.HeartbeatManager;
import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.NameSpace;
import eye.Comm.Payload;
import eye.Comm.Request;
import eye.Comm.Response;

/**
 * A server queue exists for each connection (channel). A per-channel queue
 * isolates clients. However, with a per-client model. The server is required to
 * use a master scheduler/coordinator to span all queues to enact a QoS policy.
 * 
 * How well does the per-channel work when we think about a case where 1000+
 * connections?
 * 
 * @author gash
 * 
 */
public class PerChannelQueue implements ChannelQueue {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private Channel channel;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> inbound;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker oworker;
	private InboundWorker iworker;

	// not the best method to ensure uniqueness
	private ThreadGroup tgroup = new ThreadGroup("ServerQueue-" + System.nanoTime());

	protected PerChannelQueue(Channel channel) {
		this.channel = channel;
		init();
	}

	protected void init() {
		inbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		iworker = new InboundWorker(tgroup, 1, this);
		iworker.start();

		oworker = new OutboundWorker(tgroup, 1, this);
		oworker.start();

		// let the handler manage the queue's shutdown
		// register listener to receive closing of channel
		// channel.getCloseFuture().addListener(new CloseListener(this));
	}

	protected Channel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#shutdown(boolean)
	 */
	@Override
	public void shutdown(boolean hard) {
		System.out.println("server is shutting down");

		channel = null;

		if (hard) {
			// drain queues, don't allow graceful completion
			inbound.clear();
			outbound.clear();
		}

		if (iworker != null) {
			iworker.forever = false;
			if (iworker.getState() == State.BLOCKED || iworker.getState() == State.WAITING)
				iworker.interrupt();
			iworker = null;
		}

		if (oworker != null) {
			oworker.forever = false;
			if (oworker.getState() == State.BLOCKED || oworker.getState() == State.WAITING)
				oworker.interrupt();
			oworker = null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueRequest(eye.Comm.Finger)
	 */
	@Override
	public void enqueueRequest(Request req) {
		System.out.println("@@@@@@@@@@@@@@@@@@@ in enqueueRequest in perchannelqueue");
		try {
			inbound.put(req);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueResponse(eye.Comm.Response)
	 */
	@Override
	public void enqueueResponse(Response reply) {
		System.out.println("@@@@@@@@@@@@@@@@@@@ in enqueueResponse in perchannelqueue");
		if (reply == null)
			return;

		try {
			outbound.put(reply);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for reply", e);
		}
	}

	protected class OutboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public OutboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "outbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel conn = sq.channel;
			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.outbound.take();
					if (conn.isWritable()) {
						boolean rtn = false;
						if (channel != null && channel.isOpen() && channel.isWritable()) {
							ChannelFuture cf = channel.write(msg);

							// blocks on write - use listener to be async
							cf.awaitUninterruptibly();
							rtn = cf.isSuccess();
							if (!rtn)
								sq.outbound.putFirst(msg);
						}

					} else
						sq.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error("Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
	}

	protected class InboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public InboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "inbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
        public void run() {
            Channel conn = sq.channel;
            if (conn == null || !conn.isOpen()) {
                PerChannelQueue.logger.error("connection missing, no inbound communication");
                return;
            }

            while (true) {
                if (!forever && sq.inbound.size() == 0)
                    break;

                try {
                    // block until a message is enqueued
                    GeneratedMessage msg = sq.inbound.take();

                    // process request and enqueue response
                    if (msg instanceof Request) {        // this is from client
                        Request req = ((Request) msg);       
                        
//leader checking
                        
                        if(req.getBody().getFinger().getTag().indexOf("lead")!=-1){
                			System.out.println("Leader part being executed");
                			String tags[]=req.getBody().getFinger().getTag().split("[|]+");
                			HeartbeatManager.leadcli=tags[0];
                			
                			Request.Builder rb = Request.newBuilder();
                			rb.setHeader(ResourceUtil.buildHeaderFrom(req.getHeader(), ReplyStatus.SUCCESS, null));
                			Payload.Builder pb = Payload.newBuilder();
                			Finger.Builder fb = Finger.newBuilder();
                			fb.setTag(req.getBody().getFinger().getTag());
                			fb.setNumber(req.getBody().getFinger().getNumber());
                			pb.setFinger(fb.build());
                			rb.setBody(pb.build());
                			
                			eye.Comm.Request reqNew = rb.build();
                            
                            Resource rsc = ResourceFactory.getInstance().resourceInstance(reqNew.getHeader());
                           
                            Response reply = null;
                            if (rsc == null) {
                                logger.error("failed to obtain resource for " + reqNew);
                                reply = ResourceUtil.buildError(reqNew.getHeader(), ReplyStatus.FAILURE,
                                        "Request not processed");
                            } else
                            	reply = rsc.process(reqNew);
                            
                            sq.enqueueResponse(reply);
                	
                		}
                        else {
                        
                        ResultSet rs = Server.databaseStorage.findDocument(req.getBody().getDoc().getDocName(), false, 0);
                        System.out.println(req.getBody().getDoc().getDocName());                  
                       // if(req.getHeader().getTag().equalsIgnoreCase("docfind") && rs.next()){
                        if(req.getHeader().getTag().equalsIgnoreCase("docfind")){
                        	
                            int totalChunks = 0;
                               
                                //ResultSet rs = Server.databaseStorage.findDocument(req.getBody().getDoc().getDocName(), false, 0);
                                try {
                                    while(rs.next()){                       
                                   
                                        totalChunks = rs.getInt("numberofchunks");
                                     	System.out.println("TOTAL CHUNKS "+totalChunks);
                                    }
                                } catch (SQLException e) {
                                System.out.println("error getting Chunk ids "+e);
                                }
                                for(int i=0;i<totalChunks;i++){
                                	System.out.println("IN FOR LOOP OF FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"+totalChunks);
                                    Finger.Builder f = eye.Comm.Finger.newBuilder();
                                    f.setTag(req.getHeader().getTag());

                                    Document.Builder doc = eye.Comm.Document.newBuilder();
                                    doc.setDocName(req.getBody().getDoc().getDocName());
                                    doc.setChunkId(i+1);
                                   
                                    // payload containing data
                                    Request.Builder r = Request.newBuilder();
                                    eye.Comm.Payload.Builder p = Payload.newBuilder();
                                   
                                    NameSpace.Builder ns = NameSpace.newBuilder();
                                    ns.setName(req.getBody().getSpace().getName());
                                    p.setSpace(ns);
                                   
                                    p.setDoc(doc);
                                    r.setBody(p.build());
                                   
                                    // header with routing info
                                    eye.Comm.Header.Builder h = Header.newBuilder();
                                    h.setOriginator("zero");
                                    h.setTag(req.getHeader().getTag());
                                    h.setTime(System.currentTimeMillis());
                                    h.setRoutingId(eye.Comm.Header.Routing.DOCFIND);
                                    r.setHeader(h.build());
                                    
                                    if(req.getHeader().hasRemainingHopCount())
                    				{
                    					h.setRemainingHopCount(req.getHeader().getRemainingHopCount());
                    					//System.out.println(" &&&&  "+ hbldr.getOriginator() + ".... Current hop count"+ hbldr.getRemainingHopCount());
                    				}
                    				
                                    eye.Comm.Request reqNew = r.build();
                                   
                                    Resource rsc = ResourceFactory.getInstance().resourceInstance(reqNew.getHeader());
                                   
                                    Response reply = null;
                                    if (rsc == null) {
                                        logger.error("failed to obtain resource for " + reqNew);
                                        reply = ResourceUtil.buildError(reqNew.getHeader(), ReplyStatus.FAILURE,
                                                "Request not processed");
                                    } else
                                    	reply = rsc.process(reqNew);
                                    
                                    sq.enqueueResponse(reply);
                                }                                          
                           
                        }
                        else{
                            Resource rsc = ResourceFactory.getInstance().resourceInstance(req.getHeader());
                           
                            Response reply = null;
                            if (rsc == null) {
                                logger.error("failed to obtain resource for " + req);
                                reply = ResourceUtil.buildError(req.getHeader(), ReplyStatus.FAILURE,
                                        "Request not processed");
                            } else
                                reply = rsc.process(req);
                            sq.enqueueResponse(reply);
                        }
                        }
                    }
                    /*else{
                        if (msg instanceof Response) {        // this is from broadcast server response
                            Response response = ((Response) msg);
                            System.out.println("-----------  in response from broadcast server --------------  perchannelqueue");
                            // do we need to route the request?

                            // handle it locally
                            Resource rsc = ResourceFactory.getInstance().resourceInstance(response.getHeader());

                            Response reply = null;
                            if (rsc == null) {
                                logger.error("failed to obtain resource for " + response);
                                reply = ResourceUtil.buildError(response.getHeader(), ReplyStatus.FAILURE,
                                        "Request not processed");
                            } else
                                reply = rsc.process(response);

                            sq.enqueueResponse(reply);
                        }
                    }*/

                } catch (InterruptedException ie) {
                    break;
                } catch (Exception e) {
                    PerChannelQueue.logger.error("Unexpected processing failure", e);
                    break;
                }
            }

            if (!forever) {
                PerChannelQueue.logger.info("connection queue closing");
            }
        }
    }
	public class CloseListener implements ChannelFutureListener {
		private ChannelQueue sq;

		public CloseListener(ChannelQueue sq) {
			this.sq = sq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			sq.shutdown(true);
		}
	}
}
