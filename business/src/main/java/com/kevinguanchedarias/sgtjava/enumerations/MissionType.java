package com.kevinguanchedarias.sgtjava.enumerations;

public enum MissionType {
	LEVEL_UP(1), BROADCAST_MESSAGE(2), BUILD_UNIT(3), EXPLORE(4), RETURN_MISSION(5), GATHER(6), ESTABLISH_BASE(
			7), ATTACK(8), COUNTERATTACK(9), CONQUEST(10);

	private final int value;

	private MissionType(final int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	/**
	 * Determines if the specified mission is of type unit
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean isUnitMission() {
		return value >= 4 && value <= 11;
	}
}
