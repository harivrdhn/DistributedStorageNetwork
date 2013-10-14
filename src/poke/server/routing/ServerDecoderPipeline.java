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
package poke.server.routing;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

public class ServerDecoderPipeline implements ChannelPipelineFactory {

	public ServerDecoderPipeline() {
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();

		// length (4 bytes)
		// max message size is 64 Mb = 67108864 bytes
		// this defines a framer with a max of 64 Mb message, 4 bytes are the
		// length, and strip 4 bytes
		pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
				67108864, 0, 4, 0, 4));
		// pipeline.addLast("frameDecoder", new DebugFrameDecoder(67108864, 0,
		// 4, 0, 4));
		pipeline.addLast("protobufDecoder", new ProtobufDecoder(
				eye.Comm.Request.getDefaultInstance()));
		pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		pipeline.addLast("protobufEncoder", new ProtobufEncoder());

		// varint framing - java-to-java
		// pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		// pipeline.addLast("protobufDecoder", new
		// ProtobufDecoder(eye.Comm.Finger.getDefaultInstance()));
		// pipeline.addLast("frameEncoder", new
		// ProtobufVarint32LengthFieldPrepender());
		// pipeline.addLast("protobufEncoder", new ProtobufEncoder());

		// our message processor (new instance for each connection)
		pipeline.addLast("handler", new ServerHandler());

		return pipeline;
	}
}
