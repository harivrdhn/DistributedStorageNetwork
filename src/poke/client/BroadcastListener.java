package poke.client;


public interface BroadcastListener {
	/**
	 * identifies the listener - if it needs to be removed or tracked
	 * 
	 * @return
	 */
	String getListenerID();

	/**
	 * receives the message event from the client's channel
	 * 
	 * @param msg
	 */
	void onMessage(eye.Comm.Response msg);
	/**
	 * receives the message event from the client's channel
	 * 
	 * @param msg
	 */
	void onMessage(eye.Comm.Request msg);
}

