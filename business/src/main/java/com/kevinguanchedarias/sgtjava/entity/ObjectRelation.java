package com.kevinguanchedarias.sgtjava.entity;

import java.util.List;

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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

/**
 * This entity contains the id of the referenced table and the object type id
 * 
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "object_relations")
public class ObjectRelation implements SimpleIdEntity {
	private static final long serialVersionUID = -7972319667060686603L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "object_description")
	@Fetch(FetchMode.JOIN)
	private ObjectEntity object;

	@Column(name = "reference_id")
	private Integer referenceId;

	@OneToMany(mappedBy = "relation", fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.ALL)
	@Cascade({ CascadeType.DELETE, CascadeType.REFRESH })
	@Fetch(FetchMode.SELECT)
	private List<RequirementInformation> requirements;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ObjectEntity getObject() {
		return object;
	}

	public void setObject(ObjectEntity object) {
		this.object = object;
	}

	public Integer getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(Integer referenceId) {
		this.referenceId = referenceId;
	}

	public List<RequirementInformation> getRequirements() {
		return requirements;
	}

	public void setRequirements(List<RequirementInformation> requirements) {
		this.requirements = requirements;
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
		ObjectRelation other = (ObjectRelation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
