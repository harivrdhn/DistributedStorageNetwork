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
package poke.server.resources;

import eye.Comm.Document;
import eye.Comm.Header;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.Payload;
import eye.Comm.Request;
import eye.Comm.Response;

public class ResourceUtil {

	/**
	 * Build a forwarding request message. Note this will return null if the
	 * server has already seen the request.
	 * 
	 * @param req
	 *            The request to forward
	 * @param cfg
	 *            The server's configuration
	 * @return The request with this server added to the routing path or null
	 */
public static Request buildForwardMessage(Request req, String originator, String destination) {
		
		Request.Builder bldr  = null;
			
		try {
				
				bldr = Request.newBuilder(req);
				
				Document.Builder doc = eye.Comm.Document.newBuilder();
				doc.setDocName(req.getBody().getDoc().getDocName());
				
				eye.Comm.Payload.Builder p = Payload.newBuilder();	
				p.setDoc(doc);
				bldr.setBody(p.build());
				
				Header.Builder hbldr = bldr.getHeaderBuilder();
				hbldr.setOriginator(originator);
				hbldr.setTag(req.getHeader().getTag());
				hbldr.setTime(System.currentTimeMillis());
				hbldr.setRoutingId(eye.Comm.Header.Routing.DOCFIND);
				hbldr.setToNode(destination);  //toNOde in protobuff
	
				if(hbldr.hasRemainingHopCount())
				{
					
					hbldr.setRemainingHopCount(hbldr.getRemainingHopCount()-1);
					System.out.println(" &&&&  "+ hbldr.getOriginator() + ".... Current hop count"+ hbldr.getRemainingHopCount());
				}
				else
				{
					System.out.println(" &&&&  "+ hbldr.getOriginator() + ".... setting hop count 1 ");
					hbldr.setRemainingHopCount(1);  // THis is the call from client - server  ...This is max hop count to be used
				}
				bldr.setHeader(hbldr.build());
			//}
			
		} /*catch (SQLException e) {
			e.printStackTrace();
		}*/
		catch(Exception e){
			e.printStackTrace();
		}

		return bldr.build();		
	}

	/**
	 * build the response header from a request
	 * 
	 * @param reqHeader
	 * @param status
	 * @param statusMsg
	 * @return
	 */
	public static Header buildHeaderFrom(Header reqHeader, ReplyStatus status, String statusMsg) {
		return buildHeader(reqHeader.getRoutingId(), status, statusMsg, reqHeader.getOriginator(), reqHeader.getTag());
	}

	public static Header buildHeader(Routing path, ReplyStatus status, String msg, String from, String tag) {
		Header.Builder bldr = Header.newBuilder();
		bldr.setOriginator(from);
		bldr.setRoutingId(path);
		bldr.setTag(tag);
		bldr.setReplyCode(status);

		if (msg != null)
			bldr.setReplyMsg(msg);

		bldr.setTime(System.currentTimeMillis());

		return bldr.build();
	}

	public static Response buildError(Header reqHeader, ReplyStatus status, String statusMsg) {
		Response.Builder bldr = Response.newBuilder();
		Header hdr = buildHeaderFrom(reqHeader, status, statusMsg);
		bldr.setHeader(hdr);

		// TODO add logging

		return bldr.build();
	}
}
