package com.kevinguanchedarias.owgejava.entity;

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

/**
 * Represents a Faction
 *
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "factions")
public class Faction extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
	private static final long serialVersionUID = -8190094507195501651L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private Boolean hidden;

	@Column(name = "primary_resource_name")
	private String primaryResourceName;

	@Column(name = "secondary_resource_name")
	private String secondaryResourceName;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "primary_resource_image_id")
	private ImageStore primaryResourceImage;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "secondary_resource_image_id")
	private ImageStore secondaryResourceImage;

	@Column(name = "energy_name")
	private String energyName;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "energy_image_id")
	private ImageStore energyImage;

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
	@JoinColumn(name = "improvement_id")
	private Improvement improvement;

	@Column(name = "cloned_improvements")
	private Boolean clonedImprovements = false;

	@Column(name = "custom_primary_gather_percentage")
	private Float customPrimaryGatherPercentage = 0F;

	@Column(name = "custom_secondary_gather_percentage")
	private Float customSecondaryGatherPercentage = 0F;

	@OneToMany(mappedBy = "faction", fetch = FetchType.LAZY)
	private List<UserStorage> users;

	@OneToMany(mappedBy = "unitType", fetch = FetchType.LAZY)
	private List<FactionUnitType> unitTypes;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
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

	/**
	 * @return the energyImage
	 * @since 0.9.0
	 */
	public ImageStore getEnergyImage() {
		return energyImage;
	}

	/**
	 * @param energyImage the energyImage to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEnergyImage(ImageStore energyImage) {
		this.energyImage = energyImage;
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

	/**
	 * @return the primaryResourceImage
	 * @since 0.9.0
	 */
	public ImageStore getPrimaryResourceImage() {
		return primaryResourceImage;
	}

	/**
	 * @param primaryResourceImage the primaryResourceImage to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPrimaryResourceImage(ImageStore primaryResourceImage) {
		this.primaryResourceImage = primaryResourceImage;
	}

	/**
	 * @return the secondaryResourceImage
	 * @since 0.9.0
	 */
	public ImageStore getSecondaryResourceImage() {
		return secondaryResourceImage;
	}

	/**
	 * @param secondaryResourceImage the secondaryResourceImage to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSecondaryResourceImage(ImageStore secondaryResourceImage) {
		this.secondaryResourceImage = secondaryResourceImage;
	}

	public Integer getMaxPlanets() {
		return maxPlanets;
	}

	public void setMaxPlanets(Integer maxPlanets) {
		this.maxPlanets = maxPlanets;
	}

	/**
	 * @return the customPrimaryGatherPercentage
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getCustomPrimaryGatherPercentage() {
		return customPrimaryGatherPercentage;
	}

	/**
	 * @param customPrimaryGatherPercentage the customPrimaryGatherPercentage to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setCustomPrimaryGatherPercentage(Float customPrimaryGatherPercentage) {
		this.customPrimaryGatherPercentage = customPrimaryGatherPercentage;
	}

	/**
	 * @return the customSecondaryGatherPercentage
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getCustomSecondaryGatherPercentage() {
		return customSecondaryGatherPercentage;
	}

	/**
	 * @param customSecondaryGatherPercentage the customSecondaryGatherPercentage to
	 *                                        set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setCustomSecondaryGatherPercentage(Float customSecondaryGatherPercentage) {
		this.customSecondaryGatherPercentage = customSecondaryGatherPercentage;
	}

	/**
	 * @return the unitTypes
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<FactionUnitType> getUnitTypes() {
		return unitTypes;
	}

	/**
	 * @param unitTypes the unitTypes to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setUnitTypes(List<FactionUnitType> unitTypes) {
		this.unitTypes = unitTypes;
	}

}
