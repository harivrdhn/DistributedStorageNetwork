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
	package poke.client;

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

import com.google.protobuf.GeneratedMessage;

	public class BroadcastHandler extends SimpleChannelUpstreamHandler {
		protected static Logger logger = LoggerFactory.getLogger("client");
		protected ConcurrentMap<String, ClientListener> listeners = new ConcurrentHashMap<String, ClientListener>();
		private volatile Channel channel;

		public BroadcastHandler() {
			System.out.println("in constructor  ---- BroadcastHandler");
		}

		public boolean send(GeneratedMessage msg) {
			// TODO a queue is needed to prevent overloading of the socket
			// connection. For the demonstration, we don't need it
			System.out.println("**** in send function --- broadcasthandler ");
			ChannelFuture cf = channel.write(msg);
			if (cf.isDone() && !cf.isSuccess()) {
				logger.error("failed to poke!");
				return false;
			}

			return true;
		}

		public void handleMessage(eye.Comm.Response msg) {
			System.out.println("******** in handlemessage Response of BroadcastHandler");
			for (String id : listeners.keySet()) {
				ClientListener cl = listeners.get(id);

				// TODO this may need to be delegated to a thread pool to allow
				// async processing of replies
				cl.onMessage(msg);
			}
		}

		public void handleMessage(eye.Comm.Request msg) {
			System.out.println("******** in handlemessage Request of BroadcastHandler");
			for (String id : listeners.keySet()) {
				ClientListener cl = listeners.get(id);

				// TODO this may need to be delegated to a thread pool to allow
				// async processing of replies
				cl.onMessage(msg);
			}
		}

		public void addListener(ClientListener listener) {
			if (listener == null)
				return;

			listeners.putIfAbsent(listener.getListenerID(), listener);
			System.out.println("Listeners added to concurrent map -------BroadcastHandler");
		}

		@Override
		public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			channel = e.getChannel();
			super.channelOpen(ctx, e);
		}

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (channel.isConnected())
				channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}

		@Override
		public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			if (e.getState() == ChannelState.INTEREST_OPS && ((Integer) e.getValue() == Channel.OP_WRITE)
					|| (Integer) e.getValue() == Channel.OP_READ_WRITE)
				logger.warn("channel is not writable! <--------------------------------------------");
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			
			if(e.getMessage().getClass().equals(eye.Comm.Response.getDefaultInstance())){
				System.out.println("**** in messageReceived Response function --- broadcasthandler ");
				handleMessage((eye.Comm.Response) e.getMessage());
			}
			else{
				System.out.println("**** in messageReceived Request function --- broadcasthandler ");
				handleMessage((eye.Comm.Request) e.getMessage());
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			logger.error("Handler exception, closing channel", e);

			// TODO do we really want to do this? try to re-connect?
			e.getChannel().close();
		}
	}

