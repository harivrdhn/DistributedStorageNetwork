package poke.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.util.ClientUtil;
import eye.Comm.Header;
import eye.Comm.Request;

/**
 * example listener that an application would use to receive events.
 * 
 * @author gash
 * 
 */
public class BroadcastPrintListener implements BroadcastListener {
	protected static Logger logger = LoggerFactory.getLogger("broadcast");

	private String id;

	public BroadcastPrintListener(String id) {
		this.id = id;
	}

	@Override
	public String getListenerID() {
		return id;
	}

	@Override
	public void onMessage(eye.Comm.Response msg) {
		System.out.println("&&&&&&&&&&&&&&&&&&  in BroadcastPrintListener Response  &&&&&&&&&&&&&&&");
		if (logger.isDebugEnabled())
			ClientUtil.printHeader(msg.getHeader());

		if (msg.getHeader().getRoutingId() == Header.Routing.DOCFIND){
			System.out.println("******************* "+msg.getHeader().getRoutingId());
		}
		else {
			for (int i = 0, I = msg.getBody().getDocsCount(); i < I; i++)
				ClientUtil.printDocument(msg.getBody().getDocs(i));
		}
	}

	@Override
	public void onMessage(eye.Comm.Request msg) {
		
		System.out.println("&&&&&&&&&&&&&&&&&&  in BroadcastPrintListener Request  &&&&&&&&&&&&&&&");
		//if (logger.isDebugEnabled())
			ClientUtil.printHeader(msg.getHeader());

		if (msg.getHeader().getRoutingId() == Header.Routing.DOCFIND){
			System.out.println("******************* "+msg.getHeader().getRoutingId());
		}
		// TODO Auto-generated method stub
		
	}
}
