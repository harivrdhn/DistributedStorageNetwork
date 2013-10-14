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

import java.util.List;
import java.util.Properties;

import eye.Comm.Document;
import eye.Comm.NameSpace;

public class NoOpStorage implements Storage {

	@Override
	public boolean addDocument(String namespace, Document doc) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean removeDocument(String namespace, long docId) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean updateDocument(String namespace, Document doc) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public List<Document> findDocuments(String namespace, Document criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameSpace getNameSpaceInfo(long spaceId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NameSpace> findNameSpaces(NameSpace criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameSpace createNameSpace(NameSpace space) {
		// TODO Auto-generated method stub
		return space;
	}

	@Override
	public boolean removeNameSpace(long spaceId) {
		// TODO Auto-generated method stub
		return true;
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
