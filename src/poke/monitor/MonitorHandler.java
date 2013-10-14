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
package poke.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.management.ManagementQueue;

import com.google.protobuf.GeneratedMessage;

public class MonitorHandler extends SimpleChannelUpstreamHandler {
	protected static Logger logger = LoggerFactory.getLogger("monitor");

	protected ConcurrentMap<String, MonitorListener> listeners = new ConcurrentHashMap<String, MonitorListener>();
	private volatile Channel channel;

	public MonitorHandler() {
	}

	public String getNodeId() {
		if (listeners.size() > 0)
			return listeners.values().iterator().next().getListenerID();
		else if (channel != null)
			return channel.getLocalAddress().toString();
		else
			return String.valueOf(this.hashCode());
	}

	public void addListener(MonitorListener listener) {
		if (listener == null)
			return;

		listeners.putIfAbsent(listener.getListenerID(), listener);
	}

	public boolean send(GeneratedMessage msg) {
		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		ChannelFuture cf = channel.write(msg);
		if (cf.isDone() && !cf.isSuccess()) {
			logger.error("failed to poke!");
			return false;
		}

		return true;
	}

	public void handleMessage(eye.Comm.Management msg) {
		for (String id : listeners.keySet()) {
			MonitorListener ml = listeners.get(id);

			// TODO this may need to be delegated to a thread pool to allow
			// async processing of replies
			ml.onMessage(msg);
		}
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		channel = e.getChannel();
		super.channelOpen(ctx, e);

		for (String id : listeners.keySet()) {
			MonitorListener ml = listeners.get(id);
			ml.connectionReady();
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (channel.isConnected()) {
			channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);

			for (String id : listeners.keySet()) {
				MonitorListener ml = listeners.get(id);
				ml.connectionFailed();
			}
		}
	}

	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (e.getState() == ChannelState.INTEREST_OPS && ((Integer) e.getValue() == Channel.OP_WRITE)
				|| (Integer) e.getValue() == Channel.OP_READ_WRITE)
			logger.warn("channel is not writable! <--------------------------------------------");

		// TODO notify channel temporarily not writable or when recovered
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		handleMessage((eye.Comm.Management) e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error("exception: " + e.getCause(), e);

		for (String id : listeners.keySet()) {
			MonitorListener ml = listeners.get(id);
			ml.connectionFailed();
		}

		e.getChannel().close();
	}

	/**
	 * usage:
	 * 
	 * <pre>
	 * channel.getCloseFuture().addListener(new ManagementClosedListener(queue));
	 * </pre>
	 * 
	 * @author gash
	 * 
	 */
	public static class MonitorClosedListener implements ChannelFutureListener {
		// private ManagementQueue sq;

		public MonitorClosedListener(ManagementQueue sq) {
			// this.sq = sq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// if (sq != null)
			// sq.shutdown(true);
			// sq = null;
		}

	}
}
