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

import java.util.List;

import poke.server.conf.ServerConf;
import eye.Comm.Header;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.Request;
import eye.Comm.Response;
import eye.Comm.RoutingPath;

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
	public static Request buildForwardMessage(Request req, ServerConf cfg) {

		String iam = cfg.getServer().getProperty("node.id");
		List<RoutingPath> paths = req.getHeader().getPathList();
		if (paths != null) {
			// if this server has already seen this message return null
			for (RoutingPath rp : paths) {
				if (iam.equalsIgnoreCase(rp.getNode()))
					return null;
			}
		}
		
		Request.Builder bldr = Request.newBuilder(req);
		Header.Builder hbldr = bldr.getHeaderBuilder();
		RoutingPath.Builder rpb = RoutingPath.newBuilder();
		rpb.setNode(iam);
		rpb.setTime(System.currentTimeMillis());
		hbldr.addPath(rpb.build());

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
