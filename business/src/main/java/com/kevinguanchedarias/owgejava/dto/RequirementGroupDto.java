package com.kevinguanchedarias.owgejava.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class RequirementGroupDto implements WithDtoFromEntityTrait<RequirementGroup> {

	private Integer id;
	private String name;
	private List<RequirementInformationDto> requirements;

	@Override
	public void dtoFromEntity(RequirementGroup entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		if (entity.getRequirements() != null) {
			requirements = entity.getRequirements().stream().map(current -> {
				RequirementInformationDto dto = new RequirementInformationDto();
				dto.dtoFromEntity(current);
				return dto;
			}).collect(Collectors.toList());
		}
	}

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the name
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the requirements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<RequirementInformationDto> getRequirements() {
		return requirements;
	}

	/**
	 * @param requirements the requirements to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setRequirements(List<RequirementInformationDto> requirements) {
		this.requirements = requirements;
	}

}
