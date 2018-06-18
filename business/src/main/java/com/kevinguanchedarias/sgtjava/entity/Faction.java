package com.kevinguanchedarias.sgtjava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Represents a Faction
 * 
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "factions")
public class Faction extends EntityWithImage implements EntityWithImprovements {
	private static final long serialVersionUID = -8190094507195501651L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private Boolean hidden;
	private String name;
	private String description;

	@Column(name = "primary_resource_name")
	private String primaryResourceName;

	@Column(name = "secondary_resource_name")
	private String secondaryResourceName;

	@Column(name = "primary_resource_image")
	private String primaryResourceImage;

	@Column(name = "secondary_resource_image")
	private String secondaryResourceImage;

	@Column(name = "energy_name")
	private String energyName;

	@Column(name = "energy_image")
	private String energyImage;

	@Column(name = "initial_primary_resource")
	private Integer initialPrimaryResource;

	@Column(name = "initial_secondary_resource")
	private Integer initialSecondaryResource;

	@Column(name = "initial_energy")
	private Integer initialEnergy;

	@Column(name = "primary_resource_production")
	private Float primaryResourceProduction;

	@Column(name = "secondary_resource_production")
	private Float secondaryResourceProduction;

	@Column(name = "max_planets", nullable = false)
	private Integer maxPlanets;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "improvement_id")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE })
	private Improvement improvement;

	@Column(name = "cloned_improvements")
	private Boolean clonedImprovements = false;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPrimaryResourceName() {
		return primaryResourceName;
	}

	public void setPrimaryResourceName(String primaryResourceName) {
		this.primaryResourceName = primaryResourceName;
	}

	public String getSecondaryResourceName() {
		return secondaryResourceName;
	}

	public void setSecondaryResourceName(String secondaryResourceName) {
		this.secondaryResourceName = secondaryResourceName;
	}

	public String getEnergyName() {
		return energyName;
	}

	public void setEnergyName(String energyName) {
		this.energyName = energyName;
	}

	public Integer getInitialPrimaryResource() {
		return initialPrimaryResource;
	}

	public void setInitialPrimaryResource(Integer initialPrimaryResource) {
		this.initialPrimaryResource = initialPrimaryResource;
	}

	public Integer getInitialSecondaryResource() {
		return initialSecondaryResource;
	}

	public void setInitialSecondaryResource(Integer initialSecondaryResource) {
		this.initialSecondaryResource = initialSecondaryResource;
	}

	public Integer getInitialEnergy() {
		return initialEnergy;
	}

	public void setInitialEnergy(Integer initialEnergy) {
		this.initialEnergy = initialEnergy;
	}

	public Float getPrimaryResourceProduction() {
		return primaryResourceProduction;
	}

	public void setPrimaryResourceProduction(Float primaryResourceProduction) {
		this.primaryResourceProduction = primaryResourceProduction;
	}

	public Float getSecondaryResourceProduction() {
		return secondaryResourceProduction;
	}

	public void setSecondaryResourceProduction(Float secondaryResourceProduction) {
		this.secondaryResourceProduction = secondaryResourceProduction;
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

	public String getPrimaryResourceImage() {
		return primaryResourceImage;
	}

	public void setPrimaryResourceImage(String primaryResourceImage) {
		this.primaryResourceImage = primaryResourceImage;
	}

	public String getSecondaryResourceImage() {
		return secondaryResourceImage;
	}

	public void setSecondaryResourceImage(String secondaryResourceImage) {
		this.secondaryResourceImage = secondaryResourceImage;
	}

	public String getEnergyImage() {
		return energyImage;
	}

	public void setEnergyImage(String energyImage) {
		this.energyImage = energyImage;
	}

}
