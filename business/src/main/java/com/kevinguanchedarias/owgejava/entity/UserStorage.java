package com.kevinguanchedarias.owgejava.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "user_storage")
public class UserStorage implements EntityWithId<Integer> {
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

	/**
	 * @deprecated Max Energy is now a computed value
	 */
	@Deprecated
	@Column(name = "max_energy")
	private Double maxEnergy;

	@Column(nullable = false)
	private Double points = 0D;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private List<UnlockedRelation> unlockedRelations;

	@Deprecated(since = "0.8.0")
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	@Fetch(FetchMode.JOIN)
	private UserImprovement improvements;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private List<Mission> missions;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "alliance_id")
	@Fetch(FetchMode.JOIN)
	private Alliance alliance;

	@Transient
	private Double computedPrimaryResourceGenerationPerSecond;

	@Transient
	private Double computedSecondaryResourceGenerationPerSecond;

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
		} else {
			computedPrimaryResourceGenerationPerSecond = 0D;
			computedSecondaryResourceGenerationPerSecond = 0D;
		}
	}

	/**
	 * Returns a new instance of {@link UserStorage} with only id and username <br>
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

	@Override
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

	/**
	 * @deprecated Max Energy is now a computed value
	 */
	@Deprecated
	public Double getMaxEnergy() {
		return maxEnergy;
	}

	/**
	 * @deprecated Max Energy is now a computed value
	 */
	@Deprecated
	public void setMaxEnergy(Double maxEnergy) {
		this.maxEnergy = maxEnergy;
	}

	public Double getPoints() {
		return points;
	}

	public void setPoints(Double points) {
		this.points = points;
	}

	public List<UnlockedRelation> getUnlockedRelations() {
		return unlockedRelations;
	}

	public void setUnlockedRelations(List<UnlockedRelation> unlockedRelations) {
		this.unlockedRelations = unlockedRelations;
	}

	/**
	 * 
	 * @deprecated We use computed improvements
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public UserImprovement getImprovements() {
		return improvements;
	}

	/**
	 * @deprecated We use computed improvements
	 * @param improvements
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public void setImprovements(UserImprovement improvements) {
		this.improvements = improvements;
	}

	public List<Mission> getMissions() {
		return missions;
	}

	public void setMissions(List<Mission> missions) {
		this.missions = missions;
	}

	/**
	 * @since 0.7.0
	 * @return the alliance
	 */
	public Alliance getAlliance() {
		return alliance;
	}

	/**
	 * @since 0.7.0
	 * @param alliance the alliance to set
	 */
	public void setAlliance(Alliance alliance) {
		this.alliance = alliance;
	}

	public Double getComputedPrimaryResourceGenerationPerSecond() {
		return computedPrimaryResourceGenerationPerSecond;
	}

	public Double getComputedSecondaryResourceGenerationPerSecond() {
		return computedSecondaryResourceGenerationPerSecond;
	}

}
