package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;

/**
 *
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "unit_types")
public class UnitType implements EntityWithId<Integer> {
	private static final long serialVersionUID = 6571633664776386521L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;

	@Column(name = "max_count", nullable = true)
	private Long maxCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_type")
	private UnitType parent;

	@OneToMany(mappedBy = "parent")
	private List<UnitType> children;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "image_id")
	@Fetch(FetchMode.JOIN)
	private ImageStore image;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "unitType")
	private List<ImprovementUnitType> upgradeEnhancements;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_explore", nullable = false)
	private MissionSupportEnum canExplore = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_gather", nullable = false)
	private MissionSupportEnum canGather = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_establish_base", nullable = false)
	private MissionSupportEnum canEstablishBase = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_attack", nullable = false)
	private MissionSupportEnum canAttack = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_counterattack", nullable = false)
	private MissionSupportEnum canCounterattack = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_conquest", nullable = false)
	private MissionSupportEnum canConquest = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_deploy", nullable = false)
	private MissionSupportEnum canDeploy = MissionSupportEnum.ANY;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "speed_impact_group_id")
	@Fetch(FetchMode.JOIN)
	private SpeedImpactGroup speedImpactGroup;

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
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(Long maxCount) {
		this.maxCount = maxCount;
	}

	public UnitType getParent() {
		return parent;
	}

	public void setParent(UnitType parentType) {
		this.parent = parentType;
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

	public MissionSupportEnum getCanExplore() {
		return canExplore;
	}

	public void setCanExplore(MissionSupportEnum canExplore) {
		this.canExplore = canExplore;
	}

	public MissionSupportEnum getCanGather() {
		return canGather;
	}

	public void setCanGather(MissionSupportEnum canGather) {
		this.canGather = canGather;
	}

	public MissionSupportEnum getCanEstablishBase() {
		return canEstablishBase;
	}

	public void setCanEstablishBase(MissionSupportEnum canEstablishBase) {
		this.canEstablishBase = canEstablishBase;
	}

	public MissionSupportEnum getCanAttack() {
		return canAttack;
	}

	public void setCanAttack(MissionSupportEnum canAttack) {
		this.canAttack = canAttack;
	}

	public MissionSupportEnum getCanCounterattack() {
		return canCounterattack;
	}

	public void setCanCounterattack(MissionSupportEnum canCounterattack) {
		this.canCounterattack = canCounterattack;
	}

	public MissionSupportEnum getCanConquest() {
		return canConquest;
	}

	public void setCanConquest(MissionSupportEnum canConquest) {
		this.canConquest = canConquest;
	}

	public MissionSupportEnum getCanDeploy() {
		return canDeploy;
	}

	public void setCanDeploy(MissionSupportEnum canDeploy) {
		this.canDeploy = canDeploy;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnitType other = (UnitType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

}
