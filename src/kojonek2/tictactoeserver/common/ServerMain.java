package kojonek2.tictactoeserver.common;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerMain {

	Map<Integer, ConnectionToClient> connections = Collections.synchronizedMap(new HashMap<Integer, ConnectionToClient>());
	
	public static void main(String[] args) {
		new ServerMain(4554);
	}
	
	ServerMain(int port) {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (true) {
				System.out.println("active connections:" + connections.size());
				Socket clientSocket = serverSocket.accept();
				ConnectionToClient runnable = new ConnectionToClient(clientSocket, this);
				addConnection(runnable);
				Thread thread = new Thread(runnable);
				thread.start();
			}
		} catch (Exception e) {
			System.err.println("Error during listening on port " + port);
			e.printStackTrace();
		}
	}
	
	int addConnection(ConnectionToClient connection) {
		synchronized (connections) {
			int freeId = 1;
			while (connections.containsKey(freeId)) {
				freeId++;
			}
			//System.out.println("Gave new id " + freeId);
			connections.put(freeId, connection);
			connection.idOfConnection = freeId;
			return freeId;
		}
	}
	
	void announceConnectionOfPlayer(int idOfConnection, String playerName) {
		synchronized(connections) {
			connections.forEach((id, connection) -> {
				if(id != idOfConnection) {
					connection.toSendQueue.put("Player:Connected:" + idOfConnection + ":" + playerName);
				}
			});
		}
	}
	
	boolean removeConnection(int id, ConnectionToClient connection) {
		return connections.remove(id, connection);
	}
	
}
