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

// CODE CHANGES BY PADMAJA

package poke.server.storage.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.storage.Storage;

import com.google.protobuf.ByteString;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import eye.Comm.Document;
import eye.Comm.NameSpace;

public class DatabaseStorage implements Storage {
	protected static Logger logger = LoggerFactory.getLogger("database");

	public static final String sDriver = "jdbc.driver";
	public static final String sUrl = "jdbc.url";
	public static final String sUser = "jdbc.user";
	public static final String sPass = "jdbc.password";
	public static Statement stmt  = null;

	protected Properties cfg;
	protected BoneCP cpool;

	
	public DatabaseStorage() {
		init();
		System.out.println("In DB Storage costructor init()");
		System.out.println("CPOOL free connection size=" +  cpool.getTotalFree());
	}
	
	@Override
	public void init(Properties cfg) {
		// TODO Auto-generated method stub
		
	}

	public void init() {
		if (cpool != null)
			return;

		try {
			Class.forName("org.postgresql.Driver");
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl("jdbc:postgresql://localhost:5432/FileEdge");
			config.setUsername("postgres");
			config.setPassword("RAO@postgres01");
			config.setMinConnectionsPerPartition(5);
			config.setMaxConnectionsPerPartition(100);
			config.setPartitionCount(1);
			cpool = new BoneCP(config);
			System.out.println("CPOOL connection size=" +  cpool.getTotalCreatedConnections());
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gash.jdbc.repo.Repository#release()
	 */
	@Override
	public void release() {
		if (cpool == null)
			return;

		cpool.shutdown();
		cpool = null;
	}

	@Override
	public NameSpace getNameSpaceInfo(long spaceId) {
		NameSpace space = null;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to retrieve through JDBC/SQL
			// select * from space where id = spaceId
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on looking up space " + spaceId, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return space;
	}

	@Override
	public List<NameSpace> findNameSpaces(NameSpace criteria) {
		List<NameSpace> list = null;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to search through JDBC/SQL
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on find", ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return list;
	}

	@Override
	public NameSpace createNameSpace(NameSpace space) {
		if (space == null)
			return space;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to use JDBC
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on creating space " + space, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return null;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return space;
	}

	@Override
	public boolean removeNameSpace(long spaceId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addDocument(String namespace, Document doc) {
		
		return false;
	}
	
	public boolean addDocumentNew(eye.Comm.Request request) {
		Connection conn = null;
					
		String filename = request.getBody().getDoc().getDocName();
		long numberofchunks = request.getBody().getDoc().getTotalChunk();
		String namespace = request.getBody().getSpace().getName();
		long docsize = request.getBody().getDoc().getDocSize();
		long chunkID = request.getBody().getDoc().getChunkId();
		ByteString chunkvalue = request.getBody().getDoc().getChunkContent();
		byte[] chunkbyteValue = chunkvalue.toByteArray();
		System.out.println("chiunk size while uploading DB "+chunkvalue.size());
		System.out.println("chiunk size while uploading DB bytes "+chunkbyteValue.length);
		String query=null , query2 = null;
		
		try {		
			conn = cpool.getConnection();
			
			stmt = conn.createStatement();
			
			if(chunkID == 1){
				PreparedStatement ps = conn.prepareStatement("INSERT INTO metadata VALUES (?, ?, ?, ?)");
				ps.setString(3, filename);
				ps.setLong(1, numberofchunks);
				ps.setString(4,namespace);
				ps.setLong(2,docsize);
				ps.executeUpdate();
				ps.close();
				//query = "INSERT INTO metadata(filename, numberofchunks, namespace, docsize) VALUES (\'"+filename+"\',\'"+numberofchunks+"\',\'"+namespace+"\',\'"+docsize+"\')";
				System.out.println(">>>>>>>>>>>>>>" +query +"<<<<<<<<<<");
				
				//stmt.execute(query);
			}
			System.out.println("query1 executed");
				PreparedStatement ps = conn.prepareStatement("INSERT INTO database VALUES (?, ?, ?, ?, ?)");
				ps.setLong(1, chunkID);
				ps.setBytes(3, chunkbyteValue);
				ps.setLong(2, numberofchunks);
				ps.setString(4,filename);
				ps.setString(5, namespace);
				ps.executeUpdate();
				ps.close();
				//query2 = "INSERT INTO database(chunkid, chunkvalue, totalchunks, filename) VALUES (\'"+chunkID+"\',\'{"+chunkbyteValue+"}\',\'"+numberofchunks+"\',\'"+filename+"\')";
			
			System.out.println(">>>>>>>>>>>>>>" +query2 +"<<<<<<<<<<");			
			
			//stmt.execute(query2);
			System.out.println("query2 executed");
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on inserting document space " + filename );
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}
	public ResultSet findDocument(String docname) {
		init();
		Connection conn = null;
		ResultSet rs = null;
		try {
		conn = cpool.getConnection();
		stmt = conn.createStatement();
		
		String query = "select * from metadata where filename = \'"+docname+"\'";
		
		//String query = "select data from testtable where id ='"+id+"\'";
		System.out.println(">>>>>>>>>>>>>>" +query+"<<<<<<<<<<");
		rs = stmt.executeQuery(query);
		
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("couldn find document with name : "+docname);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
		} finally {
			if (conn != null) {
				try {
					conn.close();
					//cpool.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}		
		return rs;		
	}
	public boolean removeDocument(eye.Comm.Request request) {
		init();
		Connection conn = null;
		ResultSet rs = null;
		boolean executed = false;
		String docName = null;		

		try {
		conn = cpool.getConnection();
		stmt = conn.createStatement();
		docName = request.getBody().getDoc().getDocName();
		String nameSpace = request.getBody().getSpace().getName(); 		
		String query = "delete from metadata where filename = \'"+docName+"\' and namespace = \'"+nameSpace+"\'";		
		
		System.out.println(">>>>>>>>>>>>>>" +query+"<<<<<<<<<<");
		executed = stmt.execute(query);
		
		System.out.println("quey1 executed ");
		query = "delete from database where filename = \'"+docName+"'";		
		
		System.out.println(">>>>>>>>>>>>>>" +query+"<<<<<<<<<<");
		executed = stmt.execute(query);
		executed = true;
		System.out.println("query2 executed ");
		
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("couldn delete document with name : "+docName);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
		} finally {
			if (conn != null) {
				try {
					conn.close();
					//cpool.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return executed;
		
	}

	public ResultSet findDocument(String docname, boolean retrieve, int chunkID) {
        init();
        Connection conn = null;
        ResultSet rs = null;
        String query = null;
        PreparedStatement ps = null;
        try {
        conn = cpool.getConnection();
        stmt = conn.createStatement();
                
        
       
       
        if(!retrieve){
        	ps = conn.prepareStatement("select * from metadata where filename = ?");
        	 ps.setString(1, docname);
            //query = "select * from metadata where filename = \'"+docname+"\'";
        }
        else{
        	ps = conn.prepareStatement("select * from database where filename = ? and chunkid = ?");
        	ps.setString(1, docname);
        	ps.setLong(2, chunkID);
            //query = "select * from database where filename = \'"+docname+"\' and chunkid = \'"+chunkID+"\'";
        }
        rs = ps.executeQuery();        
        
       
        //String query = "select data from testtable where id ='"+id+"\'";
        System.out.println(">>>>>>>>>>>>>>" +query+"<<<<<<<<<<");
       // rs = stmt.executeQuery(query);
       
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("couldn find document : "+ex);
            try {
                conn.rollback();
            } catch (SQLException e) {
            }

            // indicate failure
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    //cpool.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }       
        return rs;       
    }
	
	@Override
	public boolean removeDocument(String namespace, long docId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateDocument(String namespace, Document doc) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Document> findDocuments(String namespace, Document criteria) {
		// TODO Auto-generated method stub
		return null;
	}
}
