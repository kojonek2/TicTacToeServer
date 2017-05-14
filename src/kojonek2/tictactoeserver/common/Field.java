package kojonek2.tictactoeserver.common;

public class Field {

	public static int lengthOfSide;

	private FieldState state = FieldState.BLANK;

	private int x;
	private int y;
	
	private GameManagerServer gameController;

	public Field(GameManagerServer gameController, int x, int y) {
		this.x = x;
		this.y = y;
		this.gameController = gameController;
	}

	public void setState(FieldState state) {
		this.state = state;
	}

	public FieldState getState() {
		return state;
	}

	// Recursive function which tells if this field and next in given direction
	// forms long enough sequence
	public boolean isWinningField(FieldState checkedState, int fieldsNeededForWin, int directionX, int directionY) {
		int XOfNextCheckedField = x + directionX;
		int YOfNextCheckedField = y + directionY;

		if (checkedState != state) {
			return false;
		}
		if (fieldsNeededForWin <= 1) {
			return true;
		}
		if (areCoordinatesCorrect(XOfNextCheckedField, YOfNextCheckedField)) {
			return false;
		}
		Field nextFieldToCheck = gameController.getField(XOfNextCheckedField, YOfNextCheckedField);
		return nextFieldToCheck.isWinningField(checkedState, fieldsNeededForWin - 1, directionX, directionY);
	}

	private boolean areCoordinatesCorrect(int x, int y) {
		int sizeOfGameBoard = gameController.getSizeOfGameBoard();
		if (x < 0 || x >= sizeOfGameBoard) {
			return true;
		}
		if (y < 0 || y >= sizeOfGameBoard) {
			return true;
		}
		return false;
	}

}
