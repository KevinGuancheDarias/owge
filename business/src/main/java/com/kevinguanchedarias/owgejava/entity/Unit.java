package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.UnitListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
import java.io.Serial;
import java.util.List;

@ToString(callSuper = true)
@Entity
@Table(name = "units")
@EntityListeners(UnitListener.class)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
    @Serial
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
    @ToString.Exclude
    private List<InterceptableSpeedGroup> interceptableSpeedGroups;
}
