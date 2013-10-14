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
package poke.server.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import eye.Comm.Document;
import eye.Comm.NameSpace;

/**
 * A memory-based storage.
 * 
 * @author gash
 * 
 */
public class InMemoryStorage implements Storage {
	private static String sNoName = "";
	private HashMap<Long, DataNameSpace> data = new HashMap<Long, DataNameSpace>();

	@Override
	public boolean addDocument(String namespace, Document doc) {
		if (doc == null)
			return false;
		DataNameSpace dns = null;
		if (namespace == null) {
			namespace = sNoName;
			NameSpace.Builder bldr = NameSpace.newBuilder();
			bldr.setId(createKey());
			bldr.setName(sNoName);
			bldr.setOwner("none");
			bldr.setCreated(System.currentTimeMillis());
			dns = new DataNameSpace(bldr.build());
			data.put(dns.nsb.getId(), dns);
		} else
			dns = lookupByName(namespace);

		if (dns == null)
			throw new RuntimeException("Unknown namspace: " + namespace);

		Long key = null;
		if (doc.hasId())
			doc.hasId();
		else {
			// note because we store the protobuf instance (read-only)
			key = createKey();
			Document.Builder bldr = Document.newBuilder(doc);
			bldr.setId(key);
			doc = bldr.build();
		}

		return dns.add(key, doc);
	}

	@Override
	public boolean removeDocument(String namespace, long docId) {
		if (namespace == null)
			namespace = sNoName;

		boolean rtn = false;
		DataNameSpace list = data.get(namespace);
		if (list != null)
			rtn = list.remove(docId);

		return rtn;
	}

	@Override
	public boolean updateDocument(String namespace, Document doc) {
		return addDocument(namespace, doc);
	}

	@Override
	public List<Document> findDocuments(String namespace, Document criteria) {
		// TODO locating documents can be have several implementations that
		// allow for exact matching to not equal to gt to lt

		// return the namespace as queries are not implemented
		DataNameSpace list = data.get(namespace);
		if (list == null)
			return null;
		else
			return new ArrayList<Document>(list.data.values());
	}

	@Override
	public eye.Comm.NameSpace getNameSpaceInfo(long spaceId) {
		DataNameSpace dns = data.get(spaceId);
		if (dns != null)
			return dns.getNameSpace();
		else
			return null;
	}

	@Override
	public List<eye.Comm.NameSpace> findNameSpaces(eye.Comm.NameSpace criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameSpace createNameSpace(eye.Comm.NameSpace space) {
		if (space == null)
			return null;

		DataNameSpace dns = lookupByName(space.getName());
		if (dns != null)
			throw new RuntimeException("Namespace already exists");

		NameSpace.Builder bldr = NameSpace.newBuilder();
		if (space.hasId()) {
			dns = data.get(space.getId());
			if (dns != null)
				throw new RuntimeException("Namespace ID already exists");
			else
				bldr.setId(space.getId());
		} else
			bldr.setId(createKey());

		bldr.setName(space.getName());
		bldr.setCreated(System.currentTimeMillis());
		bldr.setLastModified(bldr.getCreated());

		if (space.hasOwner())
			bldr.setOwner(space.getOwner());

		if (space.hasDesc())
			bldr.setDesc(space.getDesc());

		NameSpace ns = bldr.build();
		dns = new DataNameSpace(ns);
		data.put(dns.getNameSpace().getId(), dns);

		return ns;
	}

	@Override
	public boolean removeNameSpace(long spaceId) {
		DataNameSpace dns = data.remove(spaceId);
		try {
			return (dns != null);
		} finally {
			if (dns != null)
				dns.release();
			dns = null;
		}
	}

	private DataNameSpace lookupByName(String name) {
		if (name == null)
			return null;

		for (DataNameSpace dns : data.values()) {
			if (dns.getNameSpace().getName().equals(name))
				return dns;
		}
		return null;
	}

	private long createKey() {
		// TODO need key generator
		return System.nanoTime();
	}

	private static class DataNameSpace {
		// store the builder to allow continued updates to the metadata
		eye.Comm.NameSpace.Builder nsb;
		HashMap<Long, Document> data = new HashMap<Long, Document>();

		public DataNameSpace(NameSpace ns) {
			nsb = NameSpace.newBuilder(ns);
		}

		public void release() {
			if (data != null) {
				data.clear();
				data = null;
			}

			nsb = null;
		}

		public NameSpace getNameSpace() {
			return nsb.build();
		}

		public boolean add(Long key, Document doc) {
			data.put(key, doc);
			nsb.setLastModified(System.currentTimeMillis());
			return true;
		}

		public boolean remove(Long key) {
			Document doc = data.remove(key);
			if (doc == null)
				return false;
			else {
				nsb.setLastModified(System.currentTimeMillis());
				return true;
			}
		}
	}

	@Override
	public void init(Properties cfg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
}
