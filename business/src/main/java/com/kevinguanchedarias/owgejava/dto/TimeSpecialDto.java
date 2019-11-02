/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class TimeSpecialDto extends CommonDtoWithImageStore<Integer, TimeSpecial>
		implements WithDtoFromEntityTrait<TimeSpecial>, DtoWithImprovements {

	private Long duration;
	private Long rechargeTime;
	private ImprovementDto improvement;
	private ActiveTimeSpecialDto activeTimeSpecialDto;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.dto.DtoFromEntity#dtoFromEntity(java.lang
	 * .Object)
	 */
	@Override
	public void dtoFromEntity(TimeSpecial entity) {
		DtoWithImprovements.super.dtoFromEntity(entity);
		super.dtoFromEntity(entity);
	}

	/**
	 * @since 0.8.0
	 * @return the duration
	 */
	public Long getDuration() {
		return duration;
	}

	/**
	 * @since 0.8.0
	 * @param duration the duration to set
	 */
	public void setDuration(Long duration) {
		this.duration = duration;
	}

	/**
	 * @since 0.8.0
	 * @return the rechargeTime
	 */
	public Long getRechargeTime() {
		return rechargeTime;
	}

	/**
	 * @since 0.8.0
	 * @param rechargeTime the rechargeTime to set
	 */
	public void setRechargeTime(Long rechargeTime) {
		this.rechargeTime = rechargeTime;
	}

	/**
	 * @since 0.8.0
	 * @return the improvement
	 */
	@Override
	public ImprovementDto getImprovement() {
		return improvement;
	}

	/**
	 * @since 0.8.0
	 * @param improvement the improvement to set
	 */
	@Override
	public void setImprovement(ImprovementDto improvement) {
		this.improvement = improvement;
	}

	/**
	 * @return the activeTimeSpecialDto
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ActiveTimeSpecialDto getActiveTimeSpecialDto() {
		return activeTimeSpecialDto;
	}

	/**
	 * @param activeTimeSpecialDto the activeTimeSpecialDto to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setActiveTimeSpecialDto(ActiveTimeSpecialDto activeTimeSpecialDto) {
		this.activeTimeSpecialDto = activeTimeSpecialDto;
	}

}
