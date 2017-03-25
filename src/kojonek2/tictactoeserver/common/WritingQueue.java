package kojonek2.tictactoeserver.common;

import java.util.LinkedList;
import java.util.List;

public class WritingQueue {

	private List<String> queue;
	
	public WritingQueue() {
		queue = new LinkedList<String>();
	}
	
	synchronized String take() {
		while(queue.size() <= 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return queue.remove(0);
	}
	
	synchronized void put(String toSend) {
		queue.add(toSend);
		notify();
	}
	
}

