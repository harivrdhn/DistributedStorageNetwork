package poke.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eye.Comm.Management;

public class HeartPrintListener implements MonitorListener {
	protected static Logger logger = LoggerFactory.getLogger("monitor");

	private String nodeID;

	public HeartPrintListener(String nodeID) {
		this.nodeID = nodeID;
	}

	@Override
	public String getListenerID() {
		return nodeID;
	}

	@Override
	public void onMessage(Management msg) {
		if (logger.isDebugEnabled())
			logger.debug(msg.getBeat().getNodeId());

		if (msg.hasGraph()) {
			logger.info("Received graph responses from " + msg.getBeat().getNodeId());
		} else if (msg.hasBeat()) {
			logger.info("Received HB response: " + msg.getBeat().getNodeId());
		} else
			logger.error("Received management response from unexpected host: " + msg.getBeat().getNodeId());
	}

	@Override
	public void connectionFailed() {
		logger.error("Management port connection failed");
	}

	@Override
	public void connectionReady() {
		logger.info("Management port is ready to receive messages");
	}

}
