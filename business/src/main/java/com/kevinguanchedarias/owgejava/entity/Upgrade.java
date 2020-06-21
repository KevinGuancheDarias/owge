package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.owgejava.entity.listener.UpgradeListener;

/**
 * All more* are percentages excluding moreMissions
 *
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "upgrades")
@EntityListeners(UpgradeListener.class)
public class Upgrade extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
	private static final long serialVersionUID = 905268542123876248L;

	private Integer points = 0;
	private Long time = 60L;

	@Column(name = "primary_resource")
	private Integer primaryResource = 100;

	@Column(name = "secondary_resource")
	private Integer secondaryResource = 100;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "type")
	private UpgradeType type;

	@Column(name = "level_effect")
	private Float levelEffect = 20f;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "improvement_id")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE })
	private Improvement improvement;

	@Column(name = "cloned_improvements")
	private Boolean clonedImprovements = false;

	@Transient
	private List<RequirementInformation> requirements;

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Integer getPrimaryResource() {
		return primaryResource;
	}

	public void setPrimaryResource(Integer primaryResource) {
		this.primaryResource = primaryResource;
	}

	public Integer getSecondaryResource() {
		return secondaryResource;
	}

	public void setSecondaryResource(Integer secondaryResource) {
		this.secondaryResource = secondaryResource;
	}

	public UpgradeType getType() {
		return type;
	}

	public void setType(UpgradeType type) {
		this.type = type;
	}

	public Float getLevelEffect() {
		return levelEffect;
	}

	public void setLevelEffect(Float levelEffect) {
		this.levelEffect = levelEffect;
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

	/**
	 * @return the requirements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<RequirementInformation> getRequirements() {
		return requirements;
	}

	/**
	 * @param requirements the requirements to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setRequirements(List<RequirementInformation> requirements) {
		this.requirements = requirements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
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
		Upgrade other = (Upgrade) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}

}
