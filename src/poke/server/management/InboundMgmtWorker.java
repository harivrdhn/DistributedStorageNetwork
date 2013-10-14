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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.management.ManagementQueue.ManagementQueueEntry;
import eye.Comm.Management;
import eye.Comm.Network;
import eye.Comm.Network.Action;

/**
 * The inbound management worker handles the receiving of heartbeats (network
 * status) and requests to for this node to send heartbeats. Nodes making a
 * request to receive heartbeats are in essence requesting to establish an edge
 * (comm) between two nodes. On failure, the connecter must initiate a reconnect
 * - to produce the hbMgr.
 * 
 * On loss of connection: When a connection is lost, the emitter will not try to
 * establish the connection. The edge associated with the lost node is marked
 * failed and all outbound (enqueued) messages are dropped (TBD as we could
 * delay this action to allow the node to detect and re-establish the
 * connection).
 * 
 * Connections are bi-directional (reads and writes) at this time.
 * 
 * @author gash
 * 
 */
public class InboundMgmtWorker extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management");

	int workerId;
	boolean forever = true;

	public InboundMgmtWorker(ThreadGroup tgrp, int workerId) {
		super(tgrp, "inbound-mgmt-" + workerId);
		this.workerId = workerId;

		if (ManagementQueue.outbound == null)
			throw new RuntimeException("connection worker detected null queue");
	}

	@Override
	public void run() {
		while (true) {
			if (!forever && ManagementQueue.inbound.size() == 0)
				break;

			try {
				// block until a message is enqueued
				ManagementQueueEntry msg = ManagementQueue.inbound.take();
				logger.info("Inbound message received");
				Management req = (Management) msg.req;
				if (req.hasBeat()) {
					/**
					 * Incoming: this is from a node that this node requested to
					 * create a connection (edge) to. In other words, we need to
					 * track that this connection is healthy - get a hbMgr.
					 * 
					 * Incoming are connections this node establishes, which is
					 * handled by the HeartbeatConnector.
					 */
					logger.info("Heartbeat received from " + req.getBeat().getNodeId());
					HeartbeatManager.getInstance().updateInboundHB(req.getBeat().getNodeId());
				} else if (req.hasGraph()) {
					Network n = req.getGraph();
					logger.info("Network: node '" + n.getNodeId() + "' sent a " + n.getAction());

					/**
					 * Outgoing: when a node joins to another node, the
					 * connection is monitored to relay to the requester that
					 * the node (this) is active - send a hbMgr
					 */
					if (n.getAction().getNumber() == Action.NODEJOIN_VALUE) {
						if (msg.channel.isOpen()) {
							//can i cast socka?
							SocketAddress socka = msg.channel.getLocalAddress();
							if (socka != null) {
								InetSocketAddress isa = (InetSocketAddress) socka;
								logger.info("NODEJOIN: " + isa.getHostName() + ", " + isa.getPort());
								HeartbeatManager.getInstance().addOutgoingChannel(n.getNodeId(), isa.getHostName(),
										isa.getPort(), msg.channel, msg.sa);
							}
						} else
							logger.warn(n.getNodeId() + " not writable");
					} else if (n.getAction().getNumber() == Action.NODEDEAD_VALUE) {
						// hbMgr failure - node is considered dead
					} else if (n.getAction().getNumber() == Action.NODELEAVE_VALUE) {
						// not left network gracefully
					} else if (n.getAction().getNumber() == Action.ANNOUNCE_VALUE) {
						// nodes sending their info
					} else if (n.getAction().getNumber() == Action.MAP_VALUE) {
						// request to send annoucements
					}

					// may want to reply to exchange information
				} else
					logger.error("Unknown management message");

			} catch (InterruptedException ie) {
				break;
			} catch (Exception e) {
				logger.error("Unexpected processing failure", e);
				break;
			}
		}

		if (!forever) {
			logger.info("connection queue closing");
		}
	}
}
