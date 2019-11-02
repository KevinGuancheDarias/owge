/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import java.util.Date;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ActiveTimeSpecialDto implements WithDtoFromEntityTrait<ActiveTimeSpecial> {
	private Long id;
	private Integer timeSpecial;
	private TimeSpecialStateEnum state;
	private Date activationDate;
	private Date expiringDate;
	private Long pendingTime;
	private Date readyDate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait#dtoFromEntity(
	 * java.lang.Object)
	 */
	@Override
	public void dtoFromEntity(ActiveTimeSpecial entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		timeSpecial = entity.getTimeSpecial().getId();
	}

	/**
	 * @return the id
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the timeSpecial
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getTimeSpecial() {
		return timeSpecial;
	}

	/**
	 * @param timeSpecial the timeSpecial to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setTimeSpecial(Integer timeSpecial) {
		this.timeSpecial = timeSpecial;
	}

	/**
	 * @return the state
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TimeSpecialStateEnum getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setState(TimeSpecialStateEnum state) {
		this.state = state;
	}

	/**
	 * @return the activationDate
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getActivationDate() {
		return activationDate;
	}

	/**
	 * @param activationDate the activationDate to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setActivationDate(Date activationDate) {
		this.activationDate = activationDate;
	}

	/**
	 * @return the expiringDate
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getExpiringDate() {
		return expiringDate;
	}

	/**
	 * @param expiringDate the expiringDate to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setExpiringDate(Date expiringDate) {
		this.expiringDate = expiringDate;
	}

	/**
	 * @return the pendingTime
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getPendingTime() {
		return pendingTime;
	}

	/**
	 * @param pendingTime the pendingTime to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setPendingTime(Long pendingTime) {
		this.pendingTime = pendingTime;
	}

	/**
	 * @return the readyDate
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getReadyDate() {
		return readyDate;
	}

	/**
	 * @param readyDate the readyDate to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setReadyDate(Date readyDate) {
		this.readyDate = readyDate;
	}

}
