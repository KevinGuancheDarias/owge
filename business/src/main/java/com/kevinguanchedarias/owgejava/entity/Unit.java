package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.UnitListener;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@ToString(callSuper = true)
@Entity
@Table(name = "units")
@EntityListeners(UnitListener.class)
public class Unit extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
    private static final long serialVersionUID = -1923291486680931835L;

    @Column(name = "display_in_requirements")
    private Boolean hasToDisplayInRequirements;

    @Column(name = "order_number")
    private Integer order;

    private Integer points;
    private Integer time;

    @Column(name = "primary_resource")
    private Integer primaryResource;

    @Column(name = "secondary_resource")
    private Integer secondaryResource;

    private Integer energy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type")
    @Fetch(FetchMode.JOIN)
    private UnitType type;

    private Integer attack;

    @Column(name = "health", nullable = false)
    private Integer health = 1;
    private Integer shield;
    private Integer charge;

    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique = false;

    @Column(name = "can_fast_explore", nullable = false)
    private Boolean canFastExplore = false;

    @Column
    private Double speed = 0D;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "improvement_id")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    private Improvement improvement;

    @Column(name = "cloned_improvements")
    private Boolean clonedImprovements = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speed_impact_group_id")
    @Fetch(FetchMode.JOIN)
    private SpeedImpactGroup speedImpactGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attack_rule_id")
    @Fetch(FetchMode.JOIN)
    private AttackRule attackRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "critical_attack_id")
    @Fetch(FetchMode.JOIN)
    @Getter
    @Setter
    private CriticalAttack criticalAttack;

    @Column(name = "bypass_shield", nullable = false)
    private Boolean bypassShield = false;

    @Column(name = "is_invisible", nullable = false)
    private Boolean isInvisible = false;

    @OneToMany(mappedBy = "unit", fetch = FetchType.LAZY)
    private List<InterceptableSpeedGroup> interceptableSpeedGroups;

    /**
     * @return the hasToDisplayInRequirements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Boolean getHasToDisplayInRequirements() {
        return hasToDisplayInRequirements;
    }

    /**
     * @param hasToDisplayInRequirements the hasToDisplayInRequirements to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setHasToDisplayInRequirements(Boolean hasToDisplayInRequirements) {
        this.hasToDisplayInRequirements = hasToDisplayInRequirements;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
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

    public Integer getEnergy() {
        return energy;
    }

    public void setEnergy(Integer energy) {
        this.energy = energy;
    }

    public UnitType getType() {
        return type;
    }

    public void setType(UnitType type) {
        this.type = type;
    }

    public Integer getAttack() {
        return attack;
    }

    public void setAttack(Integer attack) {
        this.attack = attack;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getShield() {
        return shield;
    }

    public void setShield(Integer shield) {
        this.shield = shield;
    }

    public Integer getCharge() {
        return charge;
    }

    public void setCharge(Integer charge) {
        this.charge = charge;
    }

    public Boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }

    /**
     * @return the canFastExplore
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Boolean getCanFastExplore() {
        return canFastExplore;
    }

    /**
     * @param canFastExplore the canFastExplore to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setCanFastExplore(Boolean canFastExplore) {
        this.canFastExplore = canFastExplore;
    }

    /**
     * @return the speed
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Double getSpeed() {
        return speed;
    }

    /**
     * @param speed the speed to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setSpeed(Double speed) {
        this.speed = speed;
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
     * @return the speedImpactGroup
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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

    /**
     * @return the attackRule
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public AttackRule getAttackRule() {
        return attackRule;
    }

    /**
     * @param attackRule the attackRule to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setAttackRule(AttackRule attackRule) {
        this.attackRule = attackRule;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public Boolean getBypassShield() {
        return bypassShield;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public void setBypassShield(Boolean bypassShield) {
        this.bypassShield = bypassShield;
    }

    /**
     * @return the isInvisible
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public Boolean getIsInvisible() {
        return isInvisible;
    }

    /**
     * @param isInvisible the isInvisible to set
     * @author Kevin Guanche Darias
     * @since 0.10.0
     */
    public void setIsInvisible(Boolean isInvisible) {
        this.isInvisible = isInvisible;
    }

    /**
     * @return the interceptableSpeedGroups
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public List<InterceptableSpeedGroup> getInterceptableSpeedGroups() {
        return interceptableSpeedGroups;
    }

    /**
     * @param interceptableSpeedGroups the interceptableSpeedGroups to set
     * @author Kevin Guanche Darias
     * @since 0.10.0
     */
    public void setInterceptableSpeedGroups(List<InterceptableSpeedGroup> interceptableSpeedGroups) {
        this.interceptableSpeedGroups = interceptableSpeedGroups;
    }

}
