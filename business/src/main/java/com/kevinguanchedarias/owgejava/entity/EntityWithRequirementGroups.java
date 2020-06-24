package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface EntityWithRequirementGroups extends EntityWithRelation {
	List<RequirementGroup> getRequirementGroups();

	void setRequirementGroups(List<RequirementGroup> requirementGroups);
}
