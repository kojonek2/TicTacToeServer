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
	
	void announceConnectionToLobby(int idOfConnection, String playerName) {
		synchronized(connections) {
			connections.forEach((id, connection) -> {
				if(id != idOfConnection) {
					connection.toSendQueue.put("Player:Add:" + idOfConnection + ":" + playerName);
				}
			});
		}
	}
	
	void announceDisconnectionFromLobby(int idOfConnection) {
		synchronized(connections) {
			connections.forEach((id, connection) -> {
				if(id != idOfConnection) {
					connection.toSendQueue.put("Player:Remove:" + idOfConnection);
				}
			});
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
	
	boolean removeConnection(int id, ConnectionToClient connection) {
		announceDisconnectionFromLobby(id);
		return connections.remove(id, connection);
	}
	
	void sendAllPlayersInLobby(ConnectionToClient receiver) {
		synchronized(connections) {
			receiver.toSendQueue.put("Player:SendingAll");
			connections.forEach((id, connection) -> {
				if(id != receiver.idOfConnection) {
					receiver.toSendQueue.put("Player:Add:" + id + ":" + connection.playerName);
				}
			});
			receiver.toSendQueue.put("Player:SentAll");
		}
	}
	
	void sendQuery(int idOfTargetConnection, String query) {
		connections.get(idOfTargetConnection).toSendQueue.put(query);
	}
}
