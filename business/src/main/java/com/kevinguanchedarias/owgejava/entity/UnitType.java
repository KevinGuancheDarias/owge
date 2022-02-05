package com.kevinguanchedarias.owgejava.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
import java.io.Serial;
import java.util.List;

/**
 * The type Unit type.
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "unit_types")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class UnitType extends EntityWithMissionLimitation<Integer> {
    @Serial
    private static final long serialVersionUID = 6571633664776386521L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    private String name;

    @Column(name = "max_count")
    private Long maxCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_max_count")
    private UnitType shareMaxCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_type")
    private UnitType parent;

    @OneToMany(mappedBy = "parent")
    private List<UnitType> children;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    @Fetch(FetchMode.JOIN)
    private ImageStore image;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "unitType")
    private List<ImprovementUnitType> upgradeEnhancements;

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

    @Column(name = "has_to_inherit_improvements")
    private Boolean hasToInheritImprovements = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "type")
    private List<Unit> units;

    public UnitType() {
        super();
    }

    public UnitType(Integer id, String name, UnitType parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
    }

    public boolean hasMaxCount() {
        return maxCount != null && maxCount > 0;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Specifies the maximum units of this type that a user can have <br>
     * <b>NOTICE: Null value means unlimited</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Long maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * @return the shareMaxCount
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public UnitType getShareMaxCount() {
        return shareMaxCount;
    }

    /**
     * @param shareMaxCount the shareMaxCount to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setShareMaxCount(UnitType shareMaxCount) {
        this.shareMaxCount = shareMaxCount;
    }

    public UnitType getParent() {
        return parent;
    }

    public void setParent(UnitType parentType) {
        parent = parentType;
    }

    public List<UnitType> getChildren() {
        return children;
    }

    public void setChildren(List<UnitType> children) {
        this.children = children;
    }

    /**
     * @return the image
     * @since 0.8.0
     */
    public ImageStore getImage() {
        return image;
    }

    /**
     * @param image the image to set
     * @since 0.8.0
     */
    public void setImage(ImageStore image) {
        this.image = image;
    }

    public List<ImprovementUnitType> getUpgradeEnhancements() {
        return upgradeEnhancements;
    }

    public void setUpgradeEnhancements(List<ImprovementUnitType> upgradeEnhancements) {
        this.upgradeEnhancements = upgradeEnhancements;
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
     * Gets units.
     *
     * @return the units
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    public List<Unit> getUnits() {
        return units;
    }

    /**
     * Sets units.
     *
     * @param units the units
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    public void setUnits(List<Unit> units) {
        this.units = units;
    }

    /**
     * If true applied benefits to parent unit type will also apply to this
     *
     * @return the hasToInheritImprovements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Boolean getHasToInheritImprovements() {
        return hasToInheritImprovements;
    }

    /**
     * @param hasToInheritImprovements the hasToInheritImprovements to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setHasToInheritImprovements(Boolean hasToInheritImprovements) {
        this.hasToInheritImprovements = hasToInheritImprovements;
    }
}
