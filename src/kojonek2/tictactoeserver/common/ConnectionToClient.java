package kojonek2.tictactoeserver.common;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionToClient implements Runnable {

	Object lock1 = new Object();
	private boolean connectionEnded;
	
	private Socket clientSocket;
	private ServerMain mainServer;
	
	Timer pingTimer;
	
	Thread clientConnectionThread;
	
	WritingQueue toSendQueue;

	public ConnectionToClient(Socket socket, ServerMain mainServer) {
		clientSocket = socket;
		try {
			clientSocket.setSoTimeout(10000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.mainServer = mainServer;
		toSendQueue = new WritingQueue();
		connectionEnded = false;
		pingTimer = new Timer();
	}

	@Override
	public void run() {
		toSendQueue.put("Connected");
		
		clientConnectionThread = Thread.currentThread();
		
		new Thread(new SocketReaderServer(clientSocket, this)).start();
		new Thread(new SocketWriterServer(clientSocket, this)).start();
		
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				toSendQueue.put("ping");
			}
		};
		pingTimer.scheduleAtFixedRate(task, 4000, 4000);
	}
	
	void connectionEnded() {
		synchronized (lock1) {
			if(!connectionEnded) {
				mainServer.connections.remove(clientConnectionThread);
				System.out.println("active connections:" + mainServer.connections.size());
				connectionEnded = true;
				try {
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	boolean isConnectionEnded() {
		synchronized (lock1) {
			return connectionEnded;
		}
	}
	
	synchronized void processInput(String input) {
		String[] arguments = input.split(":");
		switch(arguments[0]) {
			case "ping": 
				//ignore
				break;
			default:
				System.out.println(input);
		}
	}

}
