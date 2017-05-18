package kojonek2.tictactoeserver.common;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionToClient implements Runnable {

	private Object lock1 = new Object();
	private Object lock2 = new Object();
	private Object lock3 = new Object();
	
	private boolean connectionEnded;
	
	private GameManagerServer gameManager;
	
	private Socket clientSocket;
	private ServerMain mainServer;
	
	private Timer pingTimer;
	
	private boolean inGame;
	private List<Invite> sentInvites;
		
	WritingQueue toSendQueue;
	
	int idOfConnection;
	String playerName;
	

	public ConnectionToClient(Socket socket, ServerMain mainServer) {
		sentInvites = new ArrayList<Invite>();
		clientSocket = socket;
		try {
			clientSocket.setSoTimeout(10000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.mainServer = mainServer;
		toSendQueue = new WritingQueue();
		connectionEnded = false;
		inGame = false;
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
				if(isInGame()) {
					gameManager.endGameForOtherPlayer(this);
				}
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
	
	public void setInGame(boolean b) {
		synchronized (lock2) {
			inGame = b;
		}
	}
	
	public void setGameManager(GameManagerServer gameManager) {
		synchronized(lock3) {
			this.gameManager = gameManager;
		}
	}
	
	public GameManagerServer getGameManager() {
		synchronized(lock3) {
			return gameManager;
		}
	}
	
	public boolean isInGame() {
		synchronized(lock2) {
			return inGame;
		}
	}
	
	synchronized Invite getInviteTo(int idOfConnection) {
		for(int i = sentInvites.size() - 1; i >= 0; i--) {
			if(sentInvites.get(i).getIdOfInvitedPlayer() == idOfConnection) {
				return sentInvites.get(i);
			}
		}
		return null;
	}
	
	synchronized void processInput(String input) {
		String[] arguments = input.split(":");
		switch(arguments[0]) {
			case "ping": 
				//ignore
				break;
			case "Connected":
				playerName = input.replaceFirst("Connected:", "");
				setInGame(false);
				mainServer.sendAllPlayersInLobby(this);
				mainServer.announceConnectionToLobby(idOfConnection, playerName);
				break;
			case "Player":
				if(arguments[1].equals("GetAll")) {
					mainServer.sendAllPlayersInLobby(this);
				}
				break;
			case "Invite":
				if(arguments[1].equals("Send")) {
					processSendInviteInput(arguments);
				} else if(arguments[1].equals("Cancel")) {
					processCancellationOfInvite(arguments);
				} else if(arguments[1].equals("Decline")) {
					processDeclinationOfInvite(arguments);
				} else if(arguments[1].equals("Accept")) {
					processAcceptanceOfInvite(arguments);
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
		
		Invite oldInvite = getInviteTo(idOfInvitedPlayer);
		if(oldInvite != null) {
			sentInvites.remove(oldInvite);
		}
		sentInvites.add(new Invite(idOfInvitedPlayer, sizeOfGameBoard, fieldsNeededForWin, thisConnectionState, invitedPlayerState));
		
		String query = "Invite:Send:" + idOfConnection + ":";
		query += sizeOfGameBoard + ":" + fieldsNeededForWin + ":";
		query += invitedPlayerState.getValue() + ":" + thisConnectionState.getValue();
		mainServer.sendQuery(idOfInvitedPlayer, query);
	}

	synchronized void processCancellationOfInvite(String[] arguments) {
		int idOfPlayer = Integer.parseInt(arguments[2]);
		Invite toRemove = getInviteTo(idOfPlayer);
		
		if(toRemove == null) {
			//TODO: handle case where invite has been already accepted or declined!
			System.err.println("processCancellationOfInvite - invite has been already accepted or declined");
			return;
		}
		sentInvites.remove(toRemove);
		mainServer.sendQuery(idOfPlayer, "Invite:Cancel:" + idOfConnection);
	}
	
	synchronized void processDeclinationOfInvite(String[] arguments) {
		int idOfSender = Integer.parseInt(arguments[2]); //sender of invite
		Invite invite = mainServer.getInvite(idOfSender, idOfConnection);
		if(invite == null) {
			//probably player canceled invite
			System.err.println("processDeclinationOfInvite - already canceled invite");
			return;
		}
		
		int sizeOfGameBoard = Integer.parseInt(arguments[3]);
		int fieldsNeededForWin = Integer.parseInt(arguments[4]);
		FieldState thisConnectionState = FieldState.fromInt(Integer.parseInt(arguments[5]));
		FieldState senderState = FieldState.fromInt(Integer.parseInt(arguments[6]));
		//inverted order of 2 last arguments because we are checking invite send by another player
		if(!invite.checkEquality(sizeOfGameBoard, fieldsNeededForWin, senderState, thisConnectionState)) {
			//probably player send another invite;
			System.err.println("processDeclinationOfInvite - already sent andother invite");
			return;
		}
		sentInvites.remove(invite);
		mainServer.sendQuery(idOfSender, "Invite:Decline:" + idOfConnection);
	}
	
	synchronized void processAcceptanceOfInvite(String[] arguments) {
		int idOfSender = Integer.parseInt(arguments[2]); //sender of invite
		Invite invite = mainServer.getInvite(idOfSender, idOfConnection);
		if(invite == null) {
			//probably player canceled invite
			System.err.println("processAcceptationOfInvite - already canceled invite");
			return;
		}
		int sizeOfGameBoard = Integer.parseInt(arguments[3]);
		int fieldsNeededForWin = Integer.parseInt(arguments[4]);
		FieldState thisConnectionState = FieldState.fromInt(Integer.parseInt(arguments[5]));
		FieldState senderState = FieldState.fromInt(Integer.parseInt(arguments[6]));
		//inverted order of 2 last arguments because we are checking invite send by another player
		if(!invite.checkEquality(sizeOfGameBoard, fieldsNeededForWin, senderState, thisConnectionState)) {
			//probably player send another invite;
			System.err.println("processDeclinationOfInvite - already sent andother invite");
			toSendQueue.put("Invite:RejectAcceptance");
			return;
		}
		if(mainServer.connections.get(idOfSender).isInGame() || isInGame()) {
			System.err.println("processDeclinationOfInvite - sender or this player is alrady in another game");
			toSendQueue.put("Invite:RejectAcceptance");
			return;
		}
		startGameWith(idOfSender, invite);
	}
	
	synchronized void startGameWith(int idOfSender, Invite invite) {
		ConnectionToClient senderConnection = mainServer.connections.get(idOfSender);
		senderConnection.setInGame(true);
		setInGame(true);
		mainServer.announceDisconnectionFromLobby(idOfSender);
		mainServer.announceDisconnectionFromLobby(idOfConnection);
		new GameManagerServer(senderConnection, this); //constructor of GameManagerServer assigns gameManager variable
		gameManager.startGame(invite.getSizeOfGameBoard(), invite.getFieldsNeededForWin(), invite.getThisConnectionState() , invite.getInvitedPlayerState());
	}
}
