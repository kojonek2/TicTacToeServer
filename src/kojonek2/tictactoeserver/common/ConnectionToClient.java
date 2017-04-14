package kojonek2.tictactoeserver.common;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionToClient implements Runnable {

	private Object lock1 = new Object();
	private boolean connectionEnded;
	
	private Socket clientSocket;
	private ServerMain mainServer;
	
	private Timer pingTimer;
		
	WritingQueue toSendQueue;
	
	int idOfConnection;
	String playerName;
	

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
		toSendQueue.put("Connected:" + idOfConnection);
		
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
	
	void endConnection() {
		synchronized (lock1) {
			if(!connectionEnded) {
				mainServer.removeConnection(idOfConnection, this);
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
			case "Connected":
				playerName = input.replaceFirst("Connected:", "");
				mainServer.sendAllPlayersInLobby(this);
				mainServer.announceConnectionToLobby(idOfConnection, playerName);
				System.out.println(playerName + " connected with id " + idOfConnection);
				break;
			case "Player":
				if(arguments[1].equals("GetAll")) {
					mainServer.sendAllPlayersInLobby(this);
				}
				break;
			case "Invite":
				if(arguments[1].equals("Send")) {
					processSendInviteInput(arguments);
				}
				break;
			default:
				System.out.println(input);
		}
	}
	
	synchronized void processSendInviteInput(String[] arguments) {
		int idOfInvitedPlayer = Integer.parseInt(arguments[2]);
		int sizeOfGameBoard = Integer.parseInt(arguments[3]);
		int fieldsNeededForWin = Integer.parseInt(arguments[4]);
		FieldState thisConnectionState = FieldState.fromInt(Integer.parseInt(arguments[5]));
		FieldState invitedPlayerState = FieldState.fromInt(Integer.parseInt(arguments[6]));
		String query = "Invite:Send:" + idOfConnection + ":";
		query += sizeOfGameBoard + ":" + fieldsNeededForWin + ":";
		query += invitedPlayerState.getValue() + ":" + thisConnectionState.getValue();
		mainServer.sendQuery(idOfInvitedPlayer, query);
	}

}
