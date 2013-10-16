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
package poke.server.management;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf.NearestConf;
import poke.server.management.HeartbeatData.BeatStatus;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Heartbeat;
import eye.Comm.Management;

/**
 * A server could contain multiple, separate interfaces that represent different
 * overlay networks. Therefore, we will need to manage these heartbeats
 * separately. There should be one HeartbeatManager per overlay network.
 * 
 * @author gash
 * 
 */
public class HeartbeatManager extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<HeartbeatManager> instance = new AtomicReference<HeartbeatManager>();

	// frequency heartbeats are checked
	static final int sHeartRate = 5000; // msec

	String nodeId;
	ManagementQueue mqueue;
	boolean forever = true;

	ConcurrentHashMap<Channel, HeartbeatData> outgoingHB = new ConcurrentHashMap<Channel, HeartbeatData>();
	ConcurrentHashMap<String, HeartbeatData> incomingHB = new ConcurrentHashMap<String, HeartbeatData>();

	public static HeartbeatManager getInstance(String id) {
		instance.compareAndSet(null, new HeartbeatManager(id));
		return instance.get();
	}

	public static HeartbeatManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the hbMgr for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected HeartbeatManager(String nodeId) {
		this.nodeId = nodeId;
		// outgoingHB = new DefaultChannelGroup();
	}

	/**
	 * create/register expected connections that this node will make. These
	 * edges are connections this node is responsible for monitoring.
	 * 
	 * @param edges
	 */
	public void initNetwork(NearestConf edges) {
	}

	/**
	 * update information on a node we monitor
	 * 
	 * @param nodeId
	 */
	public void updateInboundHB(String nodeId) {
		HeartbeatData hd = incomingHB.get(nodeId);
		if (hd == null) {
			logger.error("Unknown hbMgr received from node ", nodeId);
			return;
		} else {
			hd.setFailures(0);
			hd.setLastBeat(System.currentTimeMillis());
		}
	}

	/**
	 * add an outgoing endpoint (send a hbMgr to). This is called when a
	 * client/server makes a request to receive heartbeats.
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	public void addOutgoingChannel(String nodeId, String host, int mgmtport, Channel ch, SocketAddress sa) {
		if (!outgoingHB.containsKey(ch)) {
			HeartbeatData heart = new HeartbeatData(nodeId, host, null, mgmtport);
			heart.setConnection(ch, sa);
			outgoingHB.put(ch, heart);

			// when the channel closes, remove it from the outgoingHB
			ch.getCloseFuture().addListener(new CloseHeartListener(heart));
		} else {
			logger.error("Received a HB connection unknown to the server, node ID = ", nodeId);
			// TODO actions?
		}
	}

	/**
	 * This is called by the HeartbeatConnector when this node requests a
	 * connection to a node, this is called to register interest in creating a
	 * connection/edge.
	 * 
	 * @param node
	 */
	protected void addNearestNode(HeartbeatData node) {
		if (node == null || node.getHost() == null || node.getMgmtport() == null) {
			logger.error("HeartbeatManager registration of edge failed, missing data");
			return;
		}

		if (!incomingHB.containsKey(node.getNodeId())) {
			logger.info("Expects to connect to node " + node.getNodeId() + " (" + node.getHost() + ", "
					+ node.getMgmtport() + ")");

			// ensure if we reuse node instances that it is not dirty.
			node.clearAll();
			node.setInitTime(System.currentTimeMillis());
			node.setStatus(BeatStatus.Init);
			incomingHB.put(node.getNodeId(), node);
		}
	}

	/**
	 * add an incoming endpoint (receive HB from). This is called when this node
	 * actually establishes the connection to the node. Prior to this call, the
	 * system will register an inactive/pending node through addIncomingNode().
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	public void addNearestNodeChannel(String nodeId, Channel ch, SocketAddress sa) {
		HeartbeatData hd = incomingHB.get(nodeId);
		if (hd != null) {
			hd.setConnection(ch, sa);
			hd.setStatus(BeatStatus.Active);

			// when the channel closes, remove it from the incomingHB list
			ch.getCloseFuture().addListener(new CloseHeartListener(hd));
		} else {
			logger.error("Received a HB ack from an unknown node, node ID = ", nodeId);
			// TODO actions?
		}
	}

	public void release() {
		forever = true;
	}

	private Management generateHB() {
		Heartbeat.Builder h = Heartbeat.newBuilder();
		h.setTimeRef(System.currentTimeMillis());
		h.setNodeId(nodeId);

		Management.Builder b = Management.newBuilder();
		b.setBeat(h.build());

		return b.build();
	}

	@Override
	public void run() {
		logger.info("starting HB manager");

		while (forever) {
			try {
				Thread.sleep(sHeartRate);

				// ignore until we have edges with other nodes
				if (outgoingHB.size() > 0) {
					// TODO verify known node's status

					// send my status (hbMgr)
					GeneratedMessage msg = null;
					for (HeartbeatData hd : outgoingHB.values()) {
						// if failed sends exceed threshold, stop sending
						if (hd.getFailuresOnSend() > HeartbeatData.sFailureToSendThresholdDefault)
							continue;

						// only generate the message if needed
						if (msg == null)
							msg = generateHB();

						try {
							hd.channel.write(msg, hd.sa);
							hd.setLastBeatSent(System.currentTimeMillis());
							hd.setFailuresOnSend(0);
							logger.info("beat sent to: " + hd.getNodeId() + " at " + hd.getHost());
						} catch (Exception e) {
							hd.incrementFailuresOnSend();
							logger.error("Failed to send HB to " + hd.getNodeId() + " at " + hd.getHost(), e);
						}
					}
				} else
					; // logger.info("No nodes to send HB");
			} catch (InterruptedException ie) {
				break;
			} catch (Exception e) {
				logger.error("Unexpected management communcation failure", e);
				break;
			}
		}

		if (!forever)
			logger.info("management outbound queue closing");
		else
			logger.info("unexpected closing of HB manager");

	}

	public class CloseHeartListener implements ChannelFutureListener {
		private HeartbeatData heart;

		public CloseHeartListener(HeartbeatData heart) {
			this.heart = heart;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (outgoingHB.containsValue(heart)) {
				logger.warn("HB outgoing channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
				outgoingHB.remove(future.getChannel());
			} else if (incomingHB.containsValue(heart)) {
				logger.warn("HB incoming channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
				incomingHB.remove(future.getChannel());
			}
		}
	}
}
