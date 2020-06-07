package com.kevinguanchedarias.owgejava.pojo.websocket;

import java.util.List;

import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;

/**
 * Represents the user missions
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class MissionWebsocketMessage {
	private int count;
	private List<UnitRunningMissionDto> myUnitMissions;

	/**
	 *
	 * @param count
	 * @param myUnitmissions
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionWebsocketMessage(int count, List<UnitRunningMissionDto> myUnitmissions) {
		super();
		this.count = count;
		myUnitMissions = myUnitmissions;
	}

	/**
	 * @return the count
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param count the count to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return the myUnitMissions
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<UnitRunningMissionDto> getMyUnitMissions() {
		return myUnitMissions;
	}

	/**
	 * @param myUnitMissions the myUnitMissions to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMyUnitMissions(List<UnitRunningMissionDto> myUnitMissions) {
		this.myUnitMissions = myUnitMissions;
	}

}
