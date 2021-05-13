package com.kevinguanchedarias.owgejava.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

/**
 * The type Unit type.
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "unit_types")
public class UnitType extends EntityWithMissionLimitation<Integer> {
	private static final long serialVersionUID = 6571633664776386521L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;

	@Column(name = "max_count")
	private Long maxCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "share_max_count")
	private UnitType shareMaxCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_type")
	private UnitType parent;

	@OneToMany(mappedBy = "parent")
	private List<UnitType> children;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id")
	@Fetch(FetchMode.JOIN)
	private ImageStore image;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "unitType")
	private List<ImprovementUnitType> upgradeEnhancements;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "speed_impact_group_id")
	@Fetch(FetchMode.JOIN)
	private SpeedImpactGroup speedImpactGroup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "attack_rule_id")
	@Fetch(FetchMode.JOIN)
	private AttackRule attackRule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "critical_attack_id")
	@Fetch(FetchMode.JOIN)
	@Getter
	@Setter
	private CriticalAttack criticalAttack;

	@Column(name = "has_to_inherit_improvements")
	private Boolean hasToInheritImprovements = false;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "type")
	private List<Unit> units;

	public UnitType() {
		super();
	}

	public UnitType(Integer id, String name, UnitType parent) {
		this.id = id;
		this.name = name;
		this.parent = parent;
	}

	public boolean hasMaxCount() {
		return maxCount != null && maxCount > 0;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Specifies the maximum units of this type that a user can have <br>
	 * <b>NOTICE: Null value means unlimited</b>
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(Long maxCount) {
		this.maxCount = maxCount;
	}

	/**
	 * @return the shareMaxCount
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UnitType getShareMaxCount() {
		return shareMaxCount;
	}

	/**
	 * @param shareMaxCount the shareMaxCount to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setShareMaxCount(UnitType shareMaxCount) {
		this.shareMaxCount = shareMaxCount;
	}

	public UnitType getParent() {
		return parent;
	}

	public void setParent(UnitType parentType) {
		parent = parentType;
	}

	public List<UnitType> getChildren() {
		return children;
	}

	public void setChildren(List<UnitType> children) {
		this.children = children;
	}

	/**
	 * @since 0.8.0
	 * @return the image
	 */
	public ImageStore getImage() {
		return image;
	}

	/**
	 * @since 0.8.0
	 * @param image the image to set
	 */
	public void setImage(ImageStore image) {
		this.image = image;
	}

	public List<ImprovementUnitType> getUpgradeEnhancements() {
		return upgradeEnhancements;
	}

	public void setUpgradeEnhancements(List<ImprovementUnitType> upgradeEnhancements) {
		this.upgradeEnhancements = upgradeEnhancements;
	}

	/**
	 * @return the speedImpactGroup
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SpeedImpactGroup getSpeedImpactGroup() {
		return speedImpactGroup;
	}

	/**
	 * @param speedImpactGroup the speedImpactGroup to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSpeedImpactGroup(SpeedImpactGroup speedImpactGroup) {
		this.speedImpactGroup = speedImpactGroup;
	}

	/**
	 * @return the attackRule
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AttackRule getAttackRule() {
		return attackRule;
	}

	/**
	 * @param attackRule the attackRule to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAttackRule(AttackRule attackRule) {
		this.attackRule = attackRule;
	}

	/**
	 * Gets units.
	 *
	 * @return the units
	 * @since 0.9.20
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<Unit> getUnits() {
		return units;
	}

	/**
	 * Sets units.
	 *
	 * @param units the units
	 * @since 0.9.20
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setUnits(List<Unit> units) {
		this.units = units;
	}

	/**
	 * If true applied benefits to parent unit type will also apply to this
	 *
	 * @return the hasToInheritImprovements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getHasToInheritImprovements() {
		return hasToInheritImprovements;
	}

	/**
	 * @param hasToInheritImprovements the hasToInheritImprovements to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setHasToInheritImprovements(Boolean hasToInheritImprovements) {
		this.hasToInheritImprovements = hasToInheritImprovements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		UnitType other = (UnitType) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

}
