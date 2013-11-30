package poke.server.storage.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseManager {
	
	PreparedStatement stmt  = null;
	
	public boolean insertDocument(String id, String document){
		
		ResultSet rs = null;
		String query = "insert into testtable values (\""+id+"\",\""+document+"\")";
		System.out.println(">>>>>>>>>>>>>>" +query+"<<<<<<<<<<");

		return false;
	}

}
