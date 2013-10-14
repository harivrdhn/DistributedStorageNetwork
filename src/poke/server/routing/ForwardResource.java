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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import eye.Comm.Finger;
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
	protected static Logger logger = LoggerFactory.getLogger("server");

	private ServerConf cfg;

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
	public Response process(Request request) {
		String nextNode = determineForwardNode(request);
		if (nextNode != null) {
			Request fwd = ResourceUtil.buildForwardMessage(request, cfg);

			// TODO forward the request

			return null;
		} else {
			Response reply = null;
			// cannot forward the message - no edge or already traveled known
			// edges

			// TODO should we just fail silently?

			Response.Builder rb = Response.newBuilder();
			PayloadReply.Builder pb = PayloadReply.newBuilder();
			Finger.Builder fb = Finger.newBuilder();
			fb.setTag(request.getBody().getFinger().getTag());
			fb.setNumber(request.getBody().getFinger().getNumber());
			pb.setFinger(fb.build());
			rb.setBody(pb.build());

			reply = rb.build();

			return reply;
		}
	}

	/**
	 * Find the nearest node that has not received the request.
	 * 
	 * TODO this should use the heartbeat to determine which node is active in
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
