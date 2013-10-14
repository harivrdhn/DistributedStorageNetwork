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

import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.queue.PerChannelQueue.InboundWorker;
import poke.server.queue.PerChannelQueue.OutboundWorker;
import eye.Comm.Request;
import eye.Comm.Response;

/**
 * A literal single queue per server allows the server to use a
 * first-come-first-fulfillment policy. This policy is not desirable as it
 * complicates enforcement of fairness and removing 'dead' tasks. Considerations
 * for a better design is a tiered (buckets) of queues and a per-client designs.
 * 
 * Note this prototype to demonstrate a single queue implementation - this class
 * is incomplete
 * 
 * @author gash
 * 
 */
public class OnlyOneQueue implements ChannelQueue {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private Channel channel;

	// problematic (static), yet effective
	private static LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> inbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
	private static LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

	// just problematic
	private static OutboundWorker oworker;
	private static InboundWorker iworker;

	// not the best method to ensure uniqueness
	private ThreadGroup tgroup = new ThreadGroup("ServerQueue-" + System.nanoTime());

	protected OnlyOneQueue(Channel channel) {
		this.channel = channel;
		init();
	}

	private void init() {
	}

	@Override
	public void shutdown(boolean hard) {
		// Two choices are possible:
		// 1. go through the queues removing messages from this channel
		// 2. mark the channel as dead, to inform the workers to ignore enqueued
		// requests/responses - not the best approach as we are forced to hold
		// onto the the channel instance. Use a hash?
	}

	@Override
	public void enqueueRequest(Request req) {
		// TODO Auto-generated method stub

	}

	@Override
	public void enqueueResponse(Response reply) {
		// TODO Auto-generated method stub

	}
}
