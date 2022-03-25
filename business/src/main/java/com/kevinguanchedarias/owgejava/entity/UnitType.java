package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
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
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
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
    @ToString.Exclude
    private UnitType shareMaxCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_type")
    @ToString.Exclude
    private UnitType parent;

    @OneToMany(mappedBy = "parent")
    @ToString.Exclude
    private List<UnitType> children;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    @Fetch(FetchMode.JOIN)
    private ImageStore image;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "unitType")
    @ToString.Exclude
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
    @ToString.Exclude
    private List<Unit> units;
    
    public UnitType(Integer id, String name, UnitType parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
    }

    public boolean hasMaxCount() {
        return maxCount != null && maxCount > 0;
    }
}
