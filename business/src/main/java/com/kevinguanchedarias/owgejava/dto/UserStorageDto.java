package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.business.util.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;

public class UserStorageDto implements DtoFromEntity<UserStorage> {

    private Integer id;
    private String username;
    private String email;
    private Double primaryResource;
    private Double secondaryResource;
    private Double consumedEnergy;
    private Double primaryResourceGenerationPerSecond;
    private Double secondaryResourceGenerationPerSecond;
    private Double maxEnergy;
    private Boolean hasSkippedTutorial;
    private FactionDto factionDto;
    private PlanetDto homePlanetDto;
    private AllianceDto alliance;
    private GroupedImprovement improvements;
    private Boolean canAlterTwitchState = false;

    private Double computedPrimaryResourceGenerationPerSecond;
    private Double computedSecondaryResourceGenerationPerSecond;
    private Double computedMaxEnergy;

    @Override
    public void dtoFromEntity(UserStorage entity) {
        EntityPojoConverterUtil.convertFromTo(this, entity);
        maxEnergy = null;
    }

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

    public Double getConsumedEnergy() {
        return consumedEnergy;
    }

    public void setConsumedEnergy(Double consumedEnergy) {
        this.consumedEnergy = consumedEnergy;
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

    public FactionDto getFactionDto() {
        return factionDto;
    }

    public void setFactionDto(FactionDto factionDto) {
        this.factionDto = factionDto;
    }

    public PlanetDto getHomePlanetDto() {
        return homePlanetDto;
    }

    public void setHomePlanetDto(PlanetDto planetDto) {
        homePlanetDto = planetDto;
    }

    /**
     * @return the alliance
     * @since 0.7.0
     */
    public AllianceDto getAlliance() {
        return alliance;
    }

    /**
     * @param alliance the alliance to set
     * @since 0.7.0
     */
    public void setAlliance(AllianceDto alliance) {
        this.alliance = alliance;
    }

    /**
     * @return the improvements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public GroupedImprovement getImprovements() {
        return improvements;
    }

    /**
     * @param improvements the improvements to set
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public void setImprovements(GroupedImprovement improvements) {
        this.improvements = improvements;
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

    public Double getComputedPrimaryResourceGenerationPerSecond() {
        return computedPrimaryResourceGenerationPerSecond;
    }

    public void setComputedPrimaryResourceGenerationPerSecond(Double computedPrimaryResourceGenerationPerSecond) {
        this.computedPrimaryResourceGenerationPerSecond = computedPrimaryResourceGenerationPerSecond;
    }

    public Double getComputedSecondaryResourceGenerationPerSecond() {
        return computedSecondaryResourceGenerationPerSecond;
    }

    public void setComputedSecondaryResourceGenerationPerSecond(Double computedSecondaryResourceGenerationPerSecond) {
        this.computedSecondaryResourceGenerationPerSecond = computedSecondaryResourceGenerationPerSecond;
    }

    public Double getComputedMaxEnergy() {
        return computedMaxEnergy;
    }

    public void setComputedMaxEnergy(Double computedMaxEnergy) {
        this.computedMaxEnergy = computedMaxEnergy;
    }

}
