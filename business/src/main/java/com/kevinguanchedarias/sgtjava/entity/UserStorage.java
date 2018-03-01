package com.kevinguanchedarias.sgtjava.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "user_storage")
public class UserStorage implements SimpleIdEntity {
	private static final long serialVersionUID = 3718075595543259580L;

	@Id
	private Integer id;

	private String username;
	private String email;

	@OneToOne
	@JoinColumn(name = "faction", nullable = false)
	@Fetch(FetchMode.JOIN)
	private Faction faction;

	@Column(name = "last_action")
	private Date lastAction;

	@OneToOne
	@JoinColumn(name = "home_planet", nullable = false)
	@Fetch(FetchMode.JOIN)
	private Planet homePlanet;

	@Column(name = "primary_resource")
	private Double primaryResource;

	@Column(name = "secondary_resource")
	private Double secondaryResource;

	@Column(nullable = false)
	private Double energy;

	@Column(name = "primary_resource_generation_per_second")
	private Double primaryResourceGenerationPerSecond;

	@Column(name = "secondary_resource_generation_per_second")
	private Double secondaryResourceGenerationPerSecond;

	@Column(name = "max_energy")
	private Double maxEnergy;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private List<UnlockedRelation> unlockedRelations;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	@Fetch(FetchMode.JOIN)
	private UserImprovement improvements;

	@Transient
	private Double computedPrimaryResourceGenerationPerSecond;

	@Transient
	private Double computedSecondaryResourceGenerationPerSecond;

	@Transient
	private Double computedMaxEnergy;

	public void addtoPrimary(Double value) {
		primaryResource += value;
	}

	public void addToSecondary(Double value) {
		secondaryResource += value;
	}

	/**
	 * Fills the transient properties of the entity <br />
	 * <b>IMPORTANT: The entity should have all the "details" </b>
	 * 
	 * @author Kevin Guanche Darias
	 */
	public void fillTransientValues() {
		if (faction != null) {
			computedPrimaryResourceGenerationPerSecond = (double) (faction.getPrimaryResourceProduction()
					+ (faction.getPrimaryResourceProduction()
							* (improvements.getMorePrimaryResourceProduction() / 100)));
			computedSecondaryResourceGenerationPerSecond = (double) (faction.getSecondaryResourceProduction()
					+ (faction.getSecondaryResourceProduction()
							* (improvements.getMoreSecondaryResourceProduction() / 100)));
			computedMaxEnergy = (double) (faction.getInitialEnergy()
					+ (faction.getInitialEnergy() * (improvements.getMoreEnergyProduction() / 100)));
		} else {
			computedPrimaryResourceGenerationPerSecond = 0D;
			computedSecondaryResourceGenerationPerSecond = 0D;
			computedMaxEnergy = 0D;
		}
	}

	/**
	 * Returns a new instance of {@link UserStorage} with only id and username
	 * <br>
	 * Potentially useful when wanting to remove ORM proxies
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorage toBasicInformation() {
		UserStorage retVal = new UserStorage();
		retVal.setId(id);
		retVal.setUsername(username);
		return retVal;
	}

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Faction getFaction() {
		return faction;
	}

	public void setFaction(Faction faction) {
		this.faction = faction;
	}

	public Date getLastAction() {
		return lastAction;
	}

	public void setLastAction(Date lastAction) {
		this.lastAction = lastAction;
	}

	public Planet getHomePlanet() {
		return homePlanet;
	}

	public void setHomePlanet(Planet homePlanet) {
		this.homePlanet = homePlanet;
	}

	public Double getPrimaryResource() {
		return primaryResource;
	}

	public void setPrimaryResource(Double primaryResource) {
		this.primaryResource = primaryResource;
	}

	public Double getSecondaryResource() {
		return secondaryResource;
	}

	public void setSecondaryResource(Double secondaryResource) {
		this.secondaryResource = secondaryResource;
	}

	public Double getEnergy() {
		return energy;
	}

	public void setEnergy(Double energy) {
		this.energy = energy;
	}

	public Double getPrimaryResourceGenerationPerSecond() {
		return primaryResourceGenerationPerSecond;
	}

	public void setPrimaryResourceGenerationPerSecond(Double primaryResourceGenerationPerSecond) {
		this.primaryResourceGenerationPerSecond = primaryResourceGenerationPerSecond;
	}

	public Double getSecondaryResourceGenerationPerSecond() {
		return secondaryResourceGenerationPerSecond;
	}

	public void setSecondaryResourceGenerationPerSecond(Double secondaryResourceGenerationPerSecond) {
		this.secondaryResourceGenerationPerSecond = secondaryResourceGenerationPerSecond;
	}

	public Double getMaxEnergy() {
		return maxEnergy;
	}

	public void setMaxEnergy(Double maxEnergy) {
		this.maxEnergy = maxEnergy;
	}

	public List<UnlockedRelation> getUnlockedRelations() {
		return unlockedRelations;
	}

	public void setUnlockedRelations(List<UnlockedRelation> unlockedRelations) {
		this.unlockedRelations = unlockedRelations;
	}

	public UserImprovement getImprovements() {
		return improvements;
	}

	public void setImprovements(UserImprovement improvements) {
		this.improvements = improvements;
	}

	public Double getComputedPrimaryResourceGenerationPerSecond() {
		return computedPrimaryResourceGenerationPerSecond;
	}

	public Double getComputedSecondaryResourceGenerationPerSecond() {
		return computedSecondaryResourceGenerationPerSecond;
	}

	public Double getComputedMaxEnergy() {
		return computedMaxEnergy;
	}

}
