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

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

public class ClientDecoderPipeline implements ChannelPipelineFactory {
	private ClientHandler handler = new ClientHandler();

	public ClientDecoderPipeline() {
	}

	public void addListener(ClientListener listener) {
		handler.addListener(listener);
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();

		// use DebugFrameDecoder to look at the raw message
		// pipeline.addLast("frameDecoder", new DebugFrameDecoder(67108864, 0,
		// 4,0, 4));

		pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(67108864, 0, 4, 0, 4));
		pipeline.addLast("protobufDecoder", new ProtobufDecoder(eye.Comm.Response.getDefaultInstance()));
		pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		pipeline.addLast("protobufEncoder", new ProtobufEncoder());

		/*
		 * if we had only java clients then this is what we can use
		 * pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		 * pipeline.addLast("protobufDecoder", new
		 * ProtobufDecoder(eye.Comm.Response.getDefaultInstance()));
		 * pipeline.addLast("frameEncoder", new
		 * ProtobufVarint32LengthFieldPrepender());
		 * pipeline.addLast("protobufEncoder", new ProtobufEncoder());
		 */

		// our message processor
		pipeline.addLast("handler", handler);

		return pipeline;
	}
}
