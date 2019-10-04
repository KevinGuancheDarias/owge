package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "improvements_unit_types")
public class ImprovementUnitType implements SimpleIdEntity {

	private static final long serialVersionUID = -6385439199243097164L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "improvement_id")
	private Improvement improvementId;

	@Column(name = "type")
	private String type;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "unit_type_id")
	private UnitType unitType;

	private Long value;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the improvementId
	 */
	public Improvement getImprovementId() {
		return improvementId;
	}

	/**
	 * @since 0.8.0
	 * @param improvementId the improvementId to set
	 */
	public void setImprovementId(Improvement improvementId) {
		this.improvementId = improvementId;
	}

	/**
	 * 
	 * @deprecated Confusing name, not matching property name, use
	 *             {@link ImprovementUnitType#getImprovementId()}
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public Improvement getUpgradeId() {
		return improvementId;
	}

	/**
	 * 
	 * @deprecated Confusing name, not matching property name, use
	 *             {@link ImprovementUnitType#setImprovementId(Improvement)}
	 * @param upgradeId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public void setUpgradeId(Improvement upgradeId) {
		this.improvementId = upgradeId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public UnitType getUnitType() {
		return unitType;
	}

	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
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
		ImprovementUnitType other = (ImprovementUnitType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
