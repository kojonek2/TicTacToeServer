package kojonek2.tictactoeserver.common;

import java.io.IOException;
import java.net.Socket;

public class ClientConnection implements Runnable {

	Object lock1 = new Object();
	private boolean connectionEnded;
	
	private Socket clientSocket;
	private ServerMain mainServer;
	
	Thread clientConnectionThread;
	
	WritingQueue toSendQueue;

	public ClientConnection(Socket socket, ServerMain mainServer) {
		clientSocket = socket;
		this.mainServer = mainServer;
		toSendQueue = new WritingQueue();
		connectionEnded = false;
	}

	@Override
	public void run() {
		clientConnectionThread = Thread.currentThread();
		
		new Thread(new SocketReaderServer(clientSocket, this)).start();
		new Thread(new SocketWriterServer(clientSocket, this)).start();
		
		toSendQueue.put("Connected");
	}
	
	void connectionEnded() {
		synchronized (lock1) {
			if(!connectionEnded) {
				mainServer.connections.remove(clientConnectionThread);
				System.out.println("active connections:" + mainServer.connections.size());
				connectionEnded = true;
				try {
					clientSocket.close();
					System.out.println("closing conection");
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
		System.out.println(input);
	}

}
