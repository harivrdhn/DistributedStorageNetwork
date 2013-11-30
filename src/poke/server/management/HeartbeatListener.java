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
package poke.server.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.MonitorListener;
import poke.server.Server;

public class HeartbeatListener implements MonitorListener {
	protected static Logger logger = LoggerFactory.getLogger("management *******************monitor *********** HEART-BEAT-LISTENER");

	private HeartbeatData data;
	
	public static int leadack=0;

	public HeartbeatListener(HeartbeatData data) {
		this.data = data;
	}

	public HeartbeatData getData() {
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.monitor.MonitorListener#getListenerID()
	 */
	@Override
	public String getListenerID() {
		return data.getNodeId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.monitor.MonitorListener#onMessage(eye.Comm.Management)
	 */
	@Override
	public void onMessage(eye.Comm.Management msg) {
		String nodes[]=msg.getBeat().getNodeId().split("[|]+");
		if(nodes.length>2){
		if(nodes[2].equals(HeartbeatManager.nodeId)){
			leadack=Integer.parseInt(nodes[3]);
		} else
			HeartbeatManager.broadcast(nodes[2], nodes[3]);
		
		if(Server.leaderNode==HeartbeatManager.nodeId){
			Leader.updateQueue(nodes[2]);
		}
		
		}
		
		if (logger.isDebugEnabled())
			logger.debug(msg.getBeat().getNodeId());

		if (msg.hasGraph()) {
			logger.info("Received graph responses");
		} else if (msg.hasBeat() && nodes[0].equals(data.getNodeId())) {
			logger.info("Received HB response from " + nodes[0]);
			
			if(nodes[1].hashCode()>HeartbeatManager.nodeId.hashCode())
				Server.leaderNode=nodes[1];
			else
				Server.leaderNode=HeartbeatManager.nodeId;
			//logger.info("Leader is set to "+Server.leaderNode+" -Hari");
		
			data.setLastBeat(System.currentTimeMillis());
		} else
			logger.error("Received hbMgr from on wrong channel or unknown host: " + msg.getBeat().getNodeId());
		
		
		
	}

	@Override
	public void connectionFailed() {
		// note a closed management port is likely to indicate the primary port
		// has failed as well
	}

	@Override
	public void connectionReady() {
		// do nothing at the moment
	}
}
