package com.kevinguanchedarias.owgejava.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

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
import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user_storage")
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserStorage implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 3718075595543259580L;

    @Id
    @EqualsAndHashCode.Include
    private Integer id;

    private String username;
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction", nullable = false)
    private Faction faction;

    @Column(name = "last_action")
    private Date lastAction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_planet", nullable = false)
    @ToString.Exclude
    private Planet homePlanet;

    @Column(name = "primary_resource")
    private Double primaryResource;

    @Column(name = "secondary_resource")
    private Double secondaryResource;

    @Column(nullable = false)
    private Double energy;

    @Column(name = "primary_resource_generation_per_second", nullable = true)
    @Deprecated(since = "0.8.0")
    private Double primaryResourceGenerationPerSecond = 0D;

    @Column(name = "secondary_resource_generation_per_second", nullable = true)
    @Deprecated(since = "0.8.0")
    private Double secondaryResourceGenerationPerSecond = 0D;

    @Column(name = "has_skipped_tutorial")
    private Boolean hasSkippedTutorial = false;

    @Column(nullable = false)
    private Double points = 0D;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<UnlockedRelation> unlockedRelations;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Mission> missions;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "alliance_id")
    private Alliance alliance;

    @Column(name = "can_alter_twitch_state", nullable = false)
    private Boolean canAlterTwitchState = false;

    private LocalDateTime lastMultiAccountCheck;
    private Float multiAccountScore;
    private boolean banned = false;

    @Transient
    @Deprecated(since = "0.8.0")
    private Double computedPrimaryResourceGenerationPerSecond;

    @Transient
    @Deprecated(since = "0.8.0")
    private Double computedSecondaryResourceGenerationPerSecond;

    public void addtoPrimary(Double value) {
        primaryResource += value;
    }

    public void addToSecondary(Double value) {
        secondaryResource += value;
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

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Not used, it's a calculated value by UserStorage
     */
    @Deprecated(since = "0.8.0")
    public Double getPrimaryResourceGenerationPerSecond() {
        return primaryResourceGenerationPerSecond;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Not used, it's a calculated value by UserStorage
     */
    @Deprecated(since = "0.8.0")
    public void setPrimaryResourceGenerationPerSecond(Double primaryResourceGenerationPerSecond) {
        this.primaryResourceGenerationPerSecond = primaryResourceGenerationPerSecond;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Not used, it's a calculated value by UserStorage
     */
    @Deprecated(since = "0.8.0")
    public Double getSecondaryResourceGenerationPerSecond() {
        return secondaryResourceGenerationPerSecond;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Not used, it's a calculated value by UserStorage
     */
    @Deprecated(since = "0.8.0")
    public void setSecondaryResourceGenerationPerSecond(Double secondaryResourceGenerationPerSecond) {
        this.secondaryResourceGenerationPerSecond = secondaryResourceGenerationPerSecond;
    }

    /**
     * @return the hasSkippedTutorial
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Boolean getHasSkippedTutorial() {
        return hasSkippedTutorial;
    }

    /**
     * @param hasSkippedTutorial the hasSkippedTutorial to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setHasSkippedTutorial(Boolean hasSkippedTutorial) {
        this.hasSkippedTutorial = hasSkippedTutorial;
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

    public List<Mission> getMissions() {
        return missions;
    }

    public void setMissions(List<Mission> missions) {
        this.missions = missions;
    }

    /**
     * @return the alliance
     * @since 0.7.0
     */
    public Alliance getAlliance() {
        return alliance;
    }

    /**
     * @param alliance the alliance to set
     * @since 0.7.0
     */
    public void setAlliance(Alliance alliance) {
        this.alliance = alliance;
    }

    /**
     * @return the canAlterTwitchState
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     */
    public Boolean getCanAlterTwitchState() {
        return canAlterTwitchState;
    }

    /**
     * @param canAlterTwitchState the canAlterTwitchState to set
     * @author Kevin Guanche Darias
     * @since 0.9.5
     */
    public void setCanAlterTwitchState(Boolean canAlterTwitchState) {
        this.canAlterTwitchState = canAlterTwitchState;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Transient properties of UserStorage are not longer required, we
     * calculate in the frontend and in the backend
     */
    @Deprecated(since = "0.8.0")
    public Double getComputedPrimaryResourceGenerationPerSecond() {
        return computedPrimaryResourceGenerationPerSecond;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Transient properties of UserStorage are not longer required, we
     * calculate in the frontend and in the backend
     */
    @Deprecated(since = "0.8.0")
    public Double getComputedSecondaryResourceGenerationPerSecond() {
        return computedSecondaryResourceGenerationPerSecond;
    }

    public LocalDateTime getLastMultiAccountCheck() {
        return lastMultiAccountCheck;
    }

    public void setLastMultiAccountCheck(LocalDateTime lastMultiAccountCheck) {
        this.lastMultiAccountCheck = lastMultiAccountCheck;
    }

    public Float getMultiAccountScore() {
        return multiAccountScore;
    }

    public void setMultiAccountScore(Float multiAccountScore) {
        this.multiAccountScore = multiAccountScore;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}
