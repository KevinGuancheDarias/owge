package com.kevinguanchedarias.owgejava.pojo;

/**
 *
 * @since 0.9.18
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class MysqlEngineInformation {
	private final String type;
	private final String status;

	public MysqlEngineInformation(String type, String status) {
		this.type = type;
		this.status = status;
	}

	/**
	 * @return the type
	 * @since 0.9.18
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the status
	 * @since 0.9.18
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "MysqlEngineInformation [type=" + type + ", status=" + status + "]";
	}

}
