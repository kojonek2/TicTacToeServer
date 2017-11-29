package kojonek2.tictactoeserver.common;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerMain {

	Map<Integer, ConnectionToClient> connections = Collections.synchronizedMap(new HashMap<Integer, ConnectionToClient>());
	
	public static void main(String[] args) {
		int port = 4554;
		if(args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				port = 4554;
			}
		} else if(args.length != 0){
			System.out.println("Passed more than one argument which should be a port number. Setting port to 4554");
		}
		new ServerMain(port);
	}
	
	ServerMain(int port) {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Local adress: " + InetAddress.getLocalHost());
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
				if(id != idOfConnection && !connection.isInGame()) {
					connection.toSendQueue.put("Player:Add:" + idOfConnection + ":" + playerName);
				}
			});
		}
	}
	
	void announceDisconnectionFromLobby(int idOfConnection) {
		synchronized(connections) {
			connections.forEach((id, connection) -> {
				if(id != idOfConnection && !connection.isInGame()) {
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
		if(!connection.isInGame()) {
			announceDisconnectionFromLobby(id);
		}
		return connections.remove(id, connection);
	}
	
	void sendAllPlayersInLobby(ConnectionToClient receiver) {
		synchronized(connections) {
			receiver.toSendQueue.put("Player:SendingAll");
			connections.forEach((id, connection) -> {
				if(id != receiver.idOfConnection && !connection.isInGame()) {
					receiver.toSendQueue.put("Player:Add:" + id + ":" + connection.playerName);
				}
			});
			receiver.toSendQueue.put("Player:SentAll");
		}
	}
	
	void sendQuery(int idOfTargetConnection, String query) {
		connections.get(idOfTargetConnection).toSendQueue.put(query);
	}
	
	Invite getInvite(int idOfSender, int idOfReceiverOfInvite) {
		return connections.get(idOfSender).getInviteTo(idOfReceiverOfInvite);
	}
}
