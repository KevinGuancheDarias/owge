/**
 * 
 */
package com.kevinguanchedarias.sgtjava.pojo;

/**
 * Represents a ranking entry
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class RankingEntry {
	private Number position;
	private Number points;
	private Number userId;
	private String username;
	private Number allianceId;
	private String allianceName;

	/**
	 * Default constructor
	 * 
	 * @param position
	 * @param points
	 * @param userId
	 * @param username
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RankingEntry(Number position, Number points, Number userId, String username) {
		super();
		this.position = position;
		this.points = points;
		this.userId = userId;
		this.username = username;
	}

	/**
	 * @since 0.7.0
	 * @return the position
	 */
	public Number getPosition() {
		return position;
	}

	/**
	 * @since 0.7.0
	 * @param position
	 *            the position to set
	 */
	public void setPosition(Number position) {
		this.position = position;
	}

	/**
	 * @since 0.7.0
	 * @return the points
	 */
	public Number getPoints() {
		return points;
	}

	/**
	 * @since 0.7.0
	 * @param points
	 *            the points to set
	 */
	public void setPoints(Number points) {
		this.points = points;
	}

	/**
	 * @since 0.7.0
	 * @return the userId
	 */
	public Number getUserId() {
		return userId;
	}

	/**
	 * @since 0.7.0
	 * @param userId
	 *            the userId to set
	 */
	public void setUserId(Number userId) {
		this.userId = userId;
	}

	/**
	 * @since 0.7.0
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @since 0.7.0
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @since 0.7.0
	 * @return the allianceId
	 */
	public Number getAllianceId() {
		return allianceId;
	}

	/**
	 * @since 0.7.0
	 * @param allianceId
	 *            the allianceId to set
	 */
	public void setAllianceId(Number allianceId) {
		this.allianceId = allianceId;
	}

	/**
	 * @since 0.7.0
	 * @return the allianceName
	 */
	public String getAllianceName() {
		return allianceName;
	}

	/**
	 * @since 0.7.0
	 * @param allianceName
	 *            the allianceName to set
	 */
	public void setAllianceName(String allianceName) {
		this.allianceName = allianceName;
	}

}
