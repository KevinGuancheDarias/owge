package com.kevinguanchedarias.owgejava.pojo;

/**
 * Represents the units that should be send to the mission
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SelectedUnit {
	private Integer id;
	private Long count;

	/**
	 * Id of the unit
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Count to send to the mission
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

}
