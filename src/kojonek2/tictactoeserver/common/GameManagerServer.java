package kojonek2.tictactoeserver.common;

import java.util.concurrent.ThreadLocalRandom;

public class GameManagerServer {

	private ConnectionToClient connectionToPlayer1; //sender of invite
	private ConnectionToClient connectionToPlayer2; //player who accepted invite

	private FieldState player1State;
	private FieldState player2State;

	private boolean randomPlayerSwaps = false;

	private int sizeOfGameBoard;
	private int fieldsNeededForWin;

	private Field[][] gameBoard;
	private FieldState playerTurn = FieldState.BLANK;

	public GameManagerServer(ConnectionToClient connectionToSender, ConnectionToClient connectionToPlayer2) {
		synchronized (connectionToSender) {
			synchronized (connectionToPlayer2) {
				this.connectionToPlayer1 = connectionToSender;
				this.connectionToPlayer2 = connectionToPlayer2;
				connectionToSender.setGameManager(this);
				connectionToPlayer2.setGameManager(this);
				connectionToSender.toSendQueue.put("Game:Start");
				connectionToPlayer2.toSendQueue.put("Game:Start");
			}
		}
	}
	
	public void startGame(int sizeOfGameBoard, int fieldsNeededForWin, FieldState player1State, FieldState player2State) {
		synchronized (connectionToPlayer1) {
			synchronized (connectionToPlayer2) {
				this.sizeOfGameBoard = sizeOfGameBoard;
				this.fieldsNeededForWin = fieldsNeededForWin;
				setPlayersStatuses(player1State, player2State);	
				createGameBoard(sizeOfGameBoard);
				nextTurn();
				sendGameInformation(connectionToPlayer1);
				sendGameInformation(connectionToPlayer2);
			}
		}
	}
	
	public void endGameForOtherPlayer(ConnectionToClient leaver) {
		synchronized (connectionToPlayer1) {
			synchronized (connectionToPlayer2) {
				if(leaver.equals(connectionToPlayer1)) {
					connectionToPlayer2.toSendQueue.put("Game:Ended:Quit");
					return;
				}
				if(leaver.equals(connectionToPlayer2)) {
					connectionToPlayer1.toSendQueue.put("Game:Ended:Quit");
					return;
				}
				System.err.println("GAmeManagerServer:endGame - there isn't player in game with this connection");
			}
		}
	}
	
	public void sendToBothPlayers(String s) {
		connectionToPlayer1.toSendQueue.put(s);
		connectionToPlayer2.toSendQueue.put(s);
	}
	
	private void setPlayersStatuses(FieldState player1State, FieldState player2State) {
		if(player1State == FieldState.RANDOM && player2State == FieldState.RANDOM) {
			randomPlayerSwaps = true;
			boolean random = ThreadLocalRandom.current().nextBoolean();
			if(random) {
				this.player1State = FieldState.CIRCLE;
				this.player2State = FieldState.CROSS;
			} else {
				this.player1State = FieldState.CROSS;
				this.player2State = FieldState.CIRCLE;
			}
			return;
		}
		if((player1State == FieldState.CIRCLE && player2State == FieldState.CROSS) ||
			(player1State == FieldState.CROSS && player2State == FieldState.CIRCLE)) {
			this.player1State = player1State;
			this.player2State = player2State;
			return;
		}
		System.err.println("GameManagerServer:setPlayerStatuses - error wrong combination of statuses");
	}
	
	private void sendGameInformation(ConnectionToClient receiver) {
		sendOpponentName(receiver);
		receiver.toSendQueue.put("Game:Info:Sending");
		String query = "Game:Info:Basic:";
		query += sizeOfGameBoard + ":" + fieldsNeededForWin + ":";
		if (connectionToPlayer1.equals(receiver)) {
			query += player1State.getValue() + ":" + player2State.getValue() + ":";
		} else if (connectionToPlayer2.equals(receiver)) {
			query += player2State.getValue() + ":" + player1State.getValue() + ":";
		}  else {
			System.err.println("GameManagerServer:sendGameInformation - player isn't in game");
			return;
		}
		query += playerTurn.getValue();
		receiver.toSendQueue.put(query);
		sendFieldsInformation(receiver);		
	}
	
	private void sendFieldsInformation(ConnectionToClient receiver) {
		for (int x = 0; x < sizeOfGameBoard; x++) {
			for (int y = 0; y < sizeOfGameBoard; y++) {
				FieldState state = gameBoard[x][y].getState();
				receiver.toSendQueue.put("Game:Info:Fields" + x + ":" + y + ":" + state.getValue());
			}
		}
		receiver.toSendQueue.put("Game:Info:Sent");
	}

	private void sendOpponentName(ConnectionToClient receiver) {
		String name;
		if (connectionToPlayer1.equals(receiver)) {
			name = connectionToPlayer2.playerName;
		} else if (connectionToPlayer2.equals(receiver)) {
			name = connectionToPlayer1.playerName;
		} else {
			name = "error";
			System.err.println("GameManagerSever:sendOpponentName - player isn't in game");
		}
		receiver.toSendQueue.put("Game:Name:" + name);
	}

	int getSizeOfGameBoard() {
		return sizeOfGameBoard;
	}

	Field getField(int x, int y) {
		return gameBoard[x][y];
	}

	FieldState getPlayerTurn() {
		return playerTurn;
	}

	private void swapPlayers() {
		FieldState temp = player1State;
		player1State = player2State;
		player2State = temp;
	}

	private void startNewGame() {
		// reverting state of the all variables to state from start of the game
		for (int x = 0; x < gameBoard.length; x++) {
			for (int y = 0; y < gameBoard.length; y++) {
				gameBoard[x][y].setState(FieldState.BLANK);
			}
		}

		playerTurn = FieldState.BLANK;

		if (randomPlayerSwaps) {
			if (ThreadLocalRandom.current().nextInt(2) == 0) {
				swapPlayers();
			}
		}

		nextTurn();
	}

	private void nextTurn() {
		if (playerTurn == FieldState.CROSS) {
			playerTurn = FieldState.CIRCLE;
		} else if (playerTurn == FieldState.CIRCLE) {
			playerTurn = FieldState.CROSS;
		} else {
			int random = ThreadLocalRandom.current().nextInt(1, 3);
			playerTurn = FieldState.fromInt(random);
		}
	}

	private void createGameBoard(int sizeOfGameBoard) {
		gameBoard = new Field[sizeOfGameBoard][sizeOfGameBoard];
		for (int x = 0; x < sizeOfGameBoard; x++) {
			for (int y = 0; y < sizeOfGameBoard; y++) {
				gameBoard[x][y] = new Field(this, x, y);
			}
		}
	}

	private FieldState findWinner() {
		for (int x = 0; x < gameBoard.length; x++) {
			for (int y = 0; y < gameBoard[x].length; y++) {

				Field field = gameBoard[x][y];
				FieldState stateOfField = field.getState();
				if (stateOfField == FieldState.CIRCLE) {
					if (isFieldCreatingWinningRow(FieldState.CIRCLE, x, y)) {
						return FieldState.CIRCLE;
					}
				} else if (stateOfField == FieldState.CROSS) {
					if (isFieldCreatingWinningRow(FieldState.CROSS, x, y)) {
						return FieldState.CROSS;
					}
				}

			}
		}
		return FieldState.BLANK;
	}

	private boolean isFieldCreatingWinningRow(FieldState stateOfField, int x, int y) {
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {

				// don't check this same spot (always generates true when i == 0
				// and j == 0)
				if (!(i == 0 && j == 0)) {
					boolean result = gameBoard[x][y].isWinningField(stateOfField, fieldsNeededForWin, i, j);
					if (result) {
						return true;
					}
				}

			}
		}
		return false;
	}

	private int getNumberOfBlankField() {
		int result = 0;
		for (Field[] array : gameBoard) {
			for (Field field : array) {
				if (field.getState() == FieldState.BLANK) {
					result++;
				}
			}
		}
		return result;
	}

	public void processInput(String input, ConnectionToClient connectionOfSender) {
		synchronized (connectionToPlayer1) {
			synchronized (connectionToPlayer2) {
				String[] arguments = input.split(":");
				
				switch (arguments[0]) {
					case "Check":
						checkPossibleEndOfGame();
						break;
					case "MadeMove":
						processMove(arguments, connectionOfSender);
						break;
					case "Quit":
						endGameForOtherPlayer(connectionOfSender);
						break;
				default:
					System.out.println("GameMenager id of connections :" + connectionToPlayer1.idOfConnection + " and "
							+ connectionToPlayer2.idOfConnection + "arguments:" + arguments);
				}
			}
		}
	}
	
	private void checkPossibleEndOfGame() {
		FieldState winner = findWinner();
		if (!(winner == FieldState.BLANK)) {
			sendToBothPlayers("Game:Ended:Winner:" + winner.getValue());
			return;
		}
		
		if (getNumberOfBlankField() <= 0) {
			sendToBothPlayers("Game:Ended:Draw");
			return;
		}
	}
	
	private void processMove(String[] arguments, ConnectionToClient connectionOfSender) {
		int x = Integer.parseInt(arguments[1]);
		int y = Integer.parseInt(arguments[2]);
		FieldState state = FieldState.fromInt(Integer.parseInt(arguments[3]));
		
		ConnectionToClient connectionOfReceiver = null; //player who needs to get information about move
		if(connectionOfSender.equals(connectionToPlayer1)) {
			connectionOfReceiver = connectionToPlayer2;
			if(player1State != state || player1State != playerTurn) {
				sendGameInformation(connectionToPlayer1);
				connectionToPlayer1.toSendQueue.put("Game:Info:Updated");
				return;
			}
		}
		if(connectionOfSender.equals(connectionToPlayer2)) {
			connectionOfReceiver = connectionToPlayer1;
			if(player2State != state || player2State != playerTurn) {
				sendGameInformation(connectionToPlayer2);
				connectionToPlayer2.toSendQueue.put("Game:Info:Updated");
				return;
			}
		}
		if(connectionOfSender == null || connectionOfReceiver == null) {
			System.err.println("GameManagerServer:processMove - fatal error bad connection");
		}
		
		Field field = gameBoard[x][y];
		if(field == null || field.getState() != FieldState.BLANK) {
			sendGameInformation(connectionOfSender);
			connectionOfSender.toSendQueue.put("Game:Info:Updated");
			return;
		}
		
		field.setState(state);
		nextTurn();
		
		connectionOfReceiver.toSendQueue.put("Game:Info:Fields:" + x + ":" + y + ":" + state.getValue());
		sendToBothPlayers("Game:Info:Turn:" + playerTurn.getValue());
		sendToBothPlayers("Game:Info:Sent");
	}
}
