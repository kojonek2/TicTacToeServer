package kojonek2.tictactoeserver.common;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerMain {

	List<Thread> connections = Collections.synchronizedList(new ArrayList<Thread>());
	
	public static void main(String[] args) {
		new ServerMain(4554);
	}
	
	ServerMain(int port) {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (true) {
				System.out.println("active connections:" + connections.size());
				Socket clientSocket = serverSocket.accept();
				Thread thread = new Thread(new ConnectionToClient(clientSocket, this));
				connections.add(thread);
				thread.start();
			}
		} catch (Exception e) {
			System.err.println("Error during listening on port " + port);
			e.printStackTrace();
		}
	}
	
}
