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
package poke.resources;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.Server;
import poke.server.management.HeartbeatListener;
import poke.server.management.HeartbeatManager;
import poke.server.management.Leader;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.routing.ForwardResource;
import poke.server.storage.jdbc.DatabaseStorage;
import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.Payload;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class PokeResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	public PokeResource() {
	}
	
	@Override
	public Response process(Response response) {
		// TODO Auto-generated method stub
		return null;
	}

	/* CODE CHANGES BY PADMAJA
	 * (non-Javadoc)
	 * 
	 * @see poke.server.resources.Resource#process(eye.Comm.Finger)
	 */
	public Response process(Request request) throws InterruptedException {
		// TODO add code to process the message/event received
		System.out.println("poke: in resource " + request.getBody().getFinger().getTag());
		ResultSet rs = null;
		Response reply = null;

		Response.Builder rb = Response.newBuilder();
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ in poke resource" + request.getBody().getFinger().getTag());
		// metadata
		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, null));

		// payload
		if(request.getBody().getFinger().getTag().equals("finger"))
		{
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, null));
				PayloadReply.Builder pb = PayloadReply.newBuilder();
				Finger.Builder fb = Finger.newBuilder();
				fb.setTag(request.getBody().getFinger().getTag());
				fb.setNumber(request.getBody().getFinger().getNumber());
				pb.setFinger(fb.build());
				rb.setBody(pb.build());
		}else if(request.getBody().getFinger().getTag().indexOf("lead")!=-1){
			String tags[]=request.getBody().getFinger().getTag().split("[|]+");
			HeartbeatManager.leadcli=tags[0];
			
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, null));
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			Finger.Builder fb = Finger.newBuilder();
			fb.setTag(request.getBody().getFinger().getTag());
			fb.setNumber(contactLead(request.getBody().getFinger().getNumber()));
			pb.setFinger(fb.build());
			rb.setBody(pb.build());
			
		}
		else if(request.getHeader().getRoutingId().toString().equalsIgnoreCase("DOCADD")) {
					
					logger.debug("DOCADD: in resource " + request.getBody().getFinger().getTag());
							
					int currentChunkID = (int) request.getBody().getDoc().getChunkId();
					int totalChunks = (int) request.getBody().getDoc().getTotalChunk();		
					boolean updated = Server.databaseStorage.addDocumentNew(request);			
								
					PayloadReply.Builder pb1 = PayloadReply.newBuilder();					                             
	                Document.Builder docs = Document.newBuilder();
	                docs.setDocName(request.getBody().getDoc().getDocName());
	                docs.setChunkId(currentChunkID);
	                docs.setTotalChunk(totalChunks);
	                docs.setChunkContent(request.getBody().getDoc().getChunkContent());
	                pb1.addDocs(docs);
					rb.setBody(pb1.build());
					Header.Builder h = Header.newBuilder();
					h.setOriginator("doc add"); 	// change to node id
					h.setTag(request.getHeader().getTag());
					h.setTime(System.currentTimeMillis());
					h.setRoutingId(request.getHeader().getRoutingId());
					if(updated)
						h.setReplyCode(ReplyStatus.SUCCESS);
					else
						h.setReplyCode(ReplyStatus.FAILURE);
					
					if(currentChunkID==totalChunks){
						if(updated)
							h.setReplyMsg(" ************  Successfully Added the File "+ request.getBody().getDoc().getDocName()+" to Database ***************");
						else
							h.setReplyMsg(" ************  Failed to Add the File "+ request.getBody().getDoc().getDocName()+"  to Database *********************");
					}else{
						if(updated)
							h.setReplyMsg(" ************  Successfully Added the File Chunk " + currentChunkID +"to Database ***************");
						else
							h.setReplyMsg(" ************  Failed to Add Chunk "+ currentChunkID+ " to the File to Database *********************");
					}
					rb.setHeader(h);
					reply = rb.build();
					
					return reply;
						
				}
				
		reply = rb.build();
		
		return reply;
	}
	
	private int contactLead(int number) throws InterruptedException {
		HeartbeatListener.leadack=number;
		
		if(Server.leaderNode==HeartbeatManager.nodeId){
			Leader.updateQueue(HeartbeatManager.nodeId);
		}
		
		while(number==0){
			number=HeartbeatListener.leadack;
		}
		
		System.out.println("it worked!!   " + number);
		return number;
	}
}
