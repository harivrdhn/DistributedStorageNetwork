package poke.server.management;

import java.util.*;

import poke.server.Server;

public class Leader extends Thread {
	
	static Queue<String> queue = new LinkedList<String>();
	
	public void run(){
		while(Server.leaderNode==HeartbeatManager.nodeId){
				if(queue.isEmpty()){
					continue;
				} else {
					String node=queue.poll();
					
					if(node==HeartbeatManager.nodeId)
						HeartbeatListener.leadack=1;
					else
						HeartbeatManager.broadcast(node,"1");
					
					System.out.println("Lead ack sent");
					
					while(HeartbeatListener.leadack!=2){
						continue;
					}
					
					System.out.println("Lead complete");
					
				}
		}
		
	}
	
	public static void updateQueue(String nodes){
		queue.add(nodes);
	}

}
