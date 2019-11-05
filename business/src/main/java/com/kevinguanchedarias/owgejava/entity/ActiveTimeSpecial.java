/**
 * 
 */
package com.kevinguanchedarias.owgejava.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;

/**
 * Represents an active TimeSpecial
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Entity
@Table(name = "active_time_specials")
public class ActiveTimeSpecial implements EntityWithId<Long> {
	private static final long serialVersionUID = 3520607499758897866L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_special_id")
	@Fetch(FetchMode.JOIN)
	private TimeSpecial timeSpecial;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TimeSpecialStateEnum state;

	@Column(name = "activation_date", nullable = false)
	private Date activationDate;

	@Column(name = "expiring_date", nullable = false)
	private Date expiringDate;

	@Column(name = "ready_date", nullable = true)
	private Date readyDate;

	@Transient
	private Long pendingTime;

	/**
	 * @return the id
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the user
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorage getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setUser(UserStorage user) {
		this.user = user;
	}

	/**
	 * @return the timeSpecial
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TimeSpecial getTimeSpecial() {
		return timeSpecial;
	}

	/**
	 * @param timeSpecial the timeSpecial to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setTimeSpecial(TimeSpecial timeSpecial) {
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
	 * The date when the effect of the time special expires, not to confuse with
	 * <i>readyDate</i>
	 * 
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
	 * The date when the active time special will be available again <br>
	 * Will only be defined when the time special is in "recharge" state
	 * 
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
