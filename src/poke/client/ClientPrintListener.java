package poke.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.util.ClientUtil;
import eye.Comm.Header;

/**
 * example listener that an application would use to receive events.
 * 
 * @author gash
 * 
 */
public class ClientPrintListener implements ClientListener {
	protected static Logger logger = LoggerFactory.getLogger("client");

	private String id;

	public ClientPrintListener(String id) {
		this.id = id;
	}

	@Override
	public String getListenerID() {
		return id;
	}

	@Override
	public void onMessage(eye.Comm.Response msg) {
		if (logger.isDebugEnabled())
			ClientUtil.printHeader(msg.getHeader());

		if (msg.getHeader().getRoutingId() == Header.Routing.FINGER)
			ClientUtil.printFinger(msg.getBody().getFinger());
		else {
			for (int i = 0, I = msg.getBody().getDocsCount(); i < I; i++)
				ClientUtil.printDocument(msg.getBody().getDocs(i));
		}
	}
}
