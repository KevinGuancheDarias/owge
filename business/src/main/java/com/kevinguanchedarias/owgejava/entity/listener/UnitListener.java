package com.kevinguanchedarias.owgejava.entity.listener;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Component
public class UnitListener {
	/**
	 * If the unit doesn't have a speedImpactGroup will use the unitType one
	 *
	 * @param unit
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostLoad
	public void loadSpeedImpactImprovement(Unit unit) {
		if (unit.getSpeedImpactGroup() == null) {
			unit.setSpeedImpactGroup(unit.getType().getSpeedImpactGroup());
		}
	}

	/**
	 * If the unit speedImpact group matches parent, will save as null
	 *
	 * @param unit
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PreUpdate
	@PrePersist
	public void putNullIfMatchsUnitType(Unit unit) {
		SpeedImpactGroup unitSpeedImpactGroup = unit.getSpeedImpactGroup();
		UnitType unitType = unit.getType();
		if (unitSpeedImpactGroup != null && unitType != null && unitType.getSpeedImpactGroup() != null
				&& unitType.getSpeedImpactGroup().getId().equals(unitSpeedImpactGroup.getId())) {
			unit.setSpeedImpactGroup(null);
		}
	}
}
