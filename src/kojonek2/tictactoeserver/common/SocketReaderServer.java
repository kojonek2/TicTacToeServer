package kojonek2.tictactoeserver.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketReaderServer implements Runnable {
	
	private ConnectionToClient clientConnection;
	private BufferedReader in;
	Socket clientSocket;

	public SocketReaderServer(Socket clientSocket, ConnectionToClient clientConnection) {
		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Error during creating SocketReader");
			e.printStackTrace();
		}
		this.clientConnection = clientConnection;
		this.clientSocket = clientSocket;
	}
	
	@Override
	public void run() {
		String inputLine;
		try {
			while((inputLine = in.readLine()) != null && !clientConnection.isConnectionEnded()) {
				clientConnection.processInput(inputLine);
			}
		} catch (IOException e) {
			//System.err.println("Error during reading inputStream");
			//e.printStackTrace();
			//ignore this exception. Probably client closed unexpectedly
		} finally {
			clientConnection.endConnection();
		}
	}
	
}
