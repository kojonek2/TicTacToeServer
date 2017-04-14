package kojonek2.tictactoeserver.common;

public enum FieldState {
	DRAW(-1),
	BLANK(0),
	CROSS(1),
	CIRCLE(2),
	RANDOM(3);
	
	private int value;
	
	private FieldState(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static FieldState fromInt(int i) {
		for(FieldState state : FieldState.values()) {
			if(state.getValue() == i) {
				return state;
			}
		}
		return null;
	}
}
