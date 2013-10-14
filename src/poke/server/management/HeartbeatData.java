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

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;

/**
 * This class contains a node's connection information and status. The
 * connection information is used to establish a connect to the specified
 * host/port to send and receive heart beat messages.
 * 
 * The information contained in this class is used by both nodes of an edge.
 * 
 * This may appear to be inverted logic as the edges logically read: given nodes
 * A and B, node A has a interest to node B. Node B will send a hbMgr to A
 * rather than A ping's node B. We implement a push model to allow A to
 * passively monitor B (its nodes of interest) though A must establish the
 * connection due to possible firewall issues.
 * 
 * As a result, the hbMgr will keep the connection between the two nodes active.
 * On connection loss, the emitting node (e.g., node B) will wait for the
 * connection to be re-established.
 * 
 * 
 * @author gash
 * 
 */
public class HeartbeatData {

	public static final int sWeakThresholdDefault = 2;
	public static final int sFailureThresholdDefault = 4;
	public static final int sFailureToSendThresholdDefault = 10;
	public static final int sBeatIntervalDefault = 10000; // msec

	private String nodeId;
	private String host;
	private Integer port;
	private Integer mgmtport;
	private BeatStatus status = BeatStatus.Unknown;
	private int beatInterval = sBeatIntervalDefault;
	private int weakTheshold = sWeakThresholdDefault;
	private int failureThreshold = sFailureThresholdDefault;
	private int failures; // TODO should be atomic
	private int failuresOnSend;
	private long initTime;
	private long lastBeat;
	private long lastBeatSent;
	private long lastFailed;

	// the connection
	public SocketAddress sa;
	public Channel channel;

	public HeartbeatData(String nodeId, String host, Integer port, Integer mgmtport) {
		this.nodeId = nodeId;
		this.host = host;
		this.port = port;
		this.mgmtport = mgmtport;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public Integer getMgmtport() {
		return mgmtport;
	}

	public void setMgmtport(Integer mgmtport) {
		this.mgmtport = mgmtport;
	}

	/**
	 * a hbMgr may not be active (established) on initialization so, we need to
	 * provide a way to initialize the metadata from the connection data
	 * 
	 * @param channel
	 * @param sa
	 */
	public void setConnection(Channel channel, SocketAddress sa) {
		this.channel = channel;
		this.sa = sa;
	}

	/**
	 * clear/reset internal tracking information and connection. The
	 * host/port/ID are retained.
	 */
	public void clearAll() {
		clearHeartData();
		initTime = 0;
		lastBeat = 0;
		lastBeatSent = 0;
		lastFailed = 0;
		failures = 0;
		failuresOnSend = 0;
		status = BeatStatus.Unknown;
	}

	public void incrementFailures() {
		failures++;
	}

	public void incrementFailuresOnSend() {
		failuresOnSend++;
	}

	public int getFailuresOnSend() {
		return failuresOnSend;
	}

	public void setFailuresOnSend(int failuresOnSend) {
		this.failuresOnSend = failuresOnSend;
	}

	public long getLastBeatSent() {
		return lastBeatSent;
	}

	public void setLastBeatSent(long lastBeatSent) {
		this.lastBeatSent = lastBeatSent;
	}

	public int getFailures() {
		return failures;
	}

	public void setFailures(int failures) {
		this.failures = failures;
	}

	public void clearHeartData() {
		// TODO if we attempt o reconnect this should be removed
		if (channel != null)
			channel.close();

		channel = null;
		sa = null;
	}

	/**
	 * verify the connection is good and if not determine if it should be
	 * disabled.
	 * 
	 * @return
	 */
	public boolean isGood() {
		if (status == BeatStatus.Active || status == BeatStatus.Weak) {
			boolean rtn = channel.isOpen() && channel.isWritable();
			if (!rtn) {
				// TODO how to use the weakThreshold and status

				lastFailed = System.currentTimeMillis();
				failures++;
				if (failures >= failureThreshold)
					status = BeatStatus.Failed;
				else if (failures >= weakTheshold)
					status = BeatStatus.Weak;
			} else {
				failures = 0;
				lastFailed = 0;
			}

			return rtn;
		} else
			return false;
	}

	/**
	 * An assigned unique key (node ID) to the remote connection
	 * 
	 * @return
	 */
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * The host to connect to
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port the remote connection is listening to
	 * 
	 * @return
	 */
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public BeatStatus getStatus() {
		return status;
	}

	public void setStatus(BeatStatus status) {
		this.status = status;
	}

	public int getWeakTheshold() {
		return weakTheshold;
	}

	public void setWeakTheshold(int weakTheshold) {
		this.weakTheshold = weakTheshold;
	}

	public int getFailureThreshold() {
		return failureThreshold;
	}

	public void setFailureThreshold(int failureThreshold) {
		this.failureThreshold = failureThreshold;
	}

	public long getInitTime() {
		return initTime;
	}

	public void setInitTime(long initTime) {
		this.initTime = initTime;
	}

	public long getLastBeat() {
		return lastBeat;
	}

	public void setLastBeat(long lastBeat) {
		this.lastBeat = lastBeat;
	}

	public long getLastFailed() {
		return lastFailed;
	}

	public void setLastFailed(long lastFailed) {
		this.lastFailed = lastFailed;
	}

	public int getBeatInterval() {
		return beatInterval;
	}

	public void setBeatInterval(int beatInterval) {
		this.beatInterval = beatInterval;
	}

	public enum BeatStatus {
		Unknown, Init, Active, Weak, Failed
	}
}
