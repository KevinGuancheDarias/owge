package com.kevinguanchedarias.owgejava.dto;

/**
 * Represents an inherited improvement from a parent unitType when the
 * subUnitType has enabled the inherit
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class InheritedImprovementUnitType {
	private UnitTypeDto parent;
	private ImprovementUnitTypeDto value;

	/**
	 * @return the parent
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UnitTypeDto getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setParent(UnitTypeDto parent) {
		this.parent = parent;
	}

	/**
	 * @return the value
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ImprovementUnitTypeDto getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setValue(ImprovementUnitTypeDto value) {
		this.value = value;
	}
}
