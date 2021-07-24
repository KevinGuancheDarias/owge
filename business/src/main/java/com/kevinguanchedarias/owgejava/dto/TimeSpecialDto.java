/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Getter
@Setter
public class TimeSpecialDto extends CommonDtoWithImageStore<Integer, TimeSpecial> implements DtoWithImprovements {

	private Long duration;
	private Long rechargeTime;
	private ImprovementDto improvement;
	private ActiveTimeSpecialDto activeTimeSpecialDto;
	private List<RequirementInformationDto> requirements;


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
}
