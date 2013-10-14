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
package poke.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import eye.Comm.Finger;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class PokeResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	public PokeResource() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.resources.Resource#process(eye.Comm.Finger)
	 */
	public Response process(Request request) {
		// TODO add code to process the message/event received
		logger.info("poke: " + request.getBody().getFinger().getTag());

		Response.Builder rb = Response.newBuilder();

		// metadata
		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(), ReplyStatus.SUCCESS, null));

		// payload
		PayloadReply.Builder pb = PayloadReply.newBuilder();
		Finger.Builder fb = Finger.newBuilder();
		fb.setTag(request.getBody().getFinger().getTag());
		fb.setNumber(request.getBody().getFinger().getNumber());
		pb.setFinger(fb.build());
		rb.setBody(pb.build());

		Response reply = rb.build();

		return reply;
	}
}
