package kojonek2.tictactoeserver.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketWriterServer implements Runnable {
	
	private ClientConnection clientConnection;
	private PrintWriter out;

	public SocketWriterServer(Socket clientSocket, ClientConnection clientConnection) {
		try {
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Error during creating SocketWriter");
			e.printStackTrace();
		}
		this.clientConnection = clientConnection;
	}
	
	@Override
	public void run() {
		while(!clientConnection.isConnectionEnded()) {
			String toSend = clientConnection.toSendQueue.take();
			out.println(toSend);
		}
	}

}