package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 *
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Entity
@Table(name = "time_specials")
public class TimeSpecial extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
	private static final long serialVersionUID = -4022925345261224355L;

	private Long duration;

	@Column(name = "recharge_time")
	private Long rechargeTime;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "improvement_id")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE })
	private Improvement improvement;

	@Column(name = "cloned_improvements")
	private Boolean clonedImprovements = false;

	/**
	 * Duration <b>in seconds</b> of the time special
	 *
	 * @since 0.8.0
	 * @return the duration
	 */
	public Long getDuration() {
		return duration;
	}

	/**
	 * Duration <b>in seconds</b> of the time special
	 *
	 * @since 0.8.0
	 * @param duration the duration to set
	 */
	public void setDuration(Long duration) {
		this.duration = duration;
	}

	/**
	 * Time to wait <b>in seconds</b> to be able to use the time special again
	 *
	 * @since 0.8.0
	 * @return the rechargeTime
	 */
	public Long getRechargeTime() {
		return rechargeTime;
	}

	/**
	 * Time to wait <b>in seconds</b> to be able to use the time special again
	 *
	 * @since 0.8.0
	 * @param rechargeTime the rechargeTime to set
	 */
	public void setRechargeTime(Long rechargeTime) {
		this.rechargeTime = rechargeTime;
	}

	@Override
	public Improvement getImprovement() {
		return improvement;
	}

	@Override
	public void setImprovement(Improvement improvement) {
		this.improvement = improvement;
	}

	@Override
	public Boolean getClonedImprovements() {
		return clonedImprovements;
	}

	@Override
	public void setClonedImprovements(Boolean clonedImprovements) {
		this.clonedImprovements = clonedImprovements;
	}

}
