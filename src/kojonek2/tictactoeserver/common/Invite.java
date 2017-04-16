package kojonek2.tictactoeserver.common;

public class Invite {

	private int idOfInvitedPlayer;
	
	private int sizeOfGameBoard;
	private int fieldsNeededForWin;
	private FieldState thisConnectionState;
	private FieldState invitedPlayerState;
	
	public Invite(int idOfInvitedPlayer, int sizeOfGameBoard, int fieldsNeededForWin, FieldState thisConnectionState, FieldState invitedPlayerState) {
		this.idOfInvitedPlayer = idOfInvitedPlayer;
		this.sizeOfGameBoard = sizeOfGameBoard;
		this.fieldsNeededForWin = fieldsNeededForWin;
		this.thisConnectionState = thisConnectionState;
		this.invitedPlayerState = invitedPlayerState;
	}
	
	public int getSizeOfGameBoard() {
		return sizeOfGameBoard;
	}
	
	public int getFieldsNeededForWin() {
		return fieldsNeededForWin;
	}
	
	public FieldState getThisConnectionState() {
		return thisConnectionState;
	}
	
	public FieldState getInvitedPlayerState() {
		return invitedPlayerState;
	}
	
	public int getIdOfInvitedPlayer() {
		return idOfInvitedPlayer;
	}
	
	public boolean checkEquality(int sizeOfGameBoard, int fieldsNeededForWin, FieldState thisConnectionState, FieldState invitedPlayerState) {
		if(this.sizeOfGameBoard != sizeOfGameBoard) return false;
		if(this.fieldsNeededForWin != fieldsNeededForWin) return false;
		if(this.thisConnectionState != thisConnectionState) return false;
		if(this.invitedPlayerState != invitedPlayerState) return false;
		return true;
	}
}
