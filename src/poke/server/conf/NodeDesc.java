package poke.server.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "nearest")
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeDesc {
	private String nodeId;
	private String host;
	private int port;
	private int mgmtPort;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMgmtPort() {
		return mgmtPort;
	}

	public void setMgmtPort(int mgmtPort) {
		this.mgmtPort = mgmtPort;
	}

}
