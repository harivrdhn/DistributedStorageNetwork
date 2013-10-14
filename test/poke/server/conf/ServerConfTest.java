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
package poke.server.conf;

import java.io.File;
import java.io.FileWriter;

import org.junit.Test;

import poke.server.conf.ServerConf.NearestConf;
import poke.server.conf.ServerConf.ResourceConf;
import eye.Comm.Header;

public class ServerConfTest {

	@Test
	public void testBasicConf() throws Exception {
		ServerConf conf = new ServerConf();
		conf.addGeneral("node.id", "zero");
		conf.addGeneral("port", "5570");
		conf.addGeneral("port.mgmt", "5670");
		conf.addGeneral("storage", "poke.server.storage.InMemoryStorage");
		conf.addGeneral("forward", "poke.server.routing.ForwardResource");

		NodeDesc node = new NodeDesc();
		node.setNodeId("one");
		node.setPort(5571);
		node.setMgmtPort(5671);
		conf.addNearestNode(node);

		node = new NodeDesc();
		node.setNodeId("two");
		node.setPort(5572);
		node.setMgmtPort(5672);
		conf.addNearestNode(node);

		ResourceConf rsc = new ResourceConf();
		rsc.setName("finger");
		rsc.setId(Header.Routing.FINGER_VALUE);
		rsc.setClazz("poke.resources.PokeResource");
		conf.addResource(rsc);

		// we can have a resource support multiple requests by having duplicate
		// entries map to the same class
		rsc = new ResourceConf();
		rsc.setName("namespace.list");
		rsc.setId(Header.Routing.NAMESPACELIST_VALUE);
		rsc.setClazz("poke.resources.NameSpaceResource");
		conf.addResource(rsc);

		rsc = new ResourceConf();
		rsc.setName("namespace.add");
		rsc.setId(Header.Routing.NAMESPACEADD_VALUE);
		rsc = new ResourceConf();
		rsc.setClazz("poke.resources.NameSpaceResource");
		conf.addResource(rsc);

		rsc.setName("namespace.remove");
		rsc.setId(Header.Routing.NAMESPACEREMOVE_VALUE);
		rsc.setClazz("poke.resources.NameSpaceResource");
		conf.addResource(rsc);

		String json = JsonUtil.encode(conf);
		FileWriter fw = null;
		try {
			fw = new FileWriter(new File("/tmp/poke.cfg"));
			fw.write(json);
			fw.close();

			System.out.println("JSON: " + json);
		} finally {
			fw.close();
		}
	}
}
