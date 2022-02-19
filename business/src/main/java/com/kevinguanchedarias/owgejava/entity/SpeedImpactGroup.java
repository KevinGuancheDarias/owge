package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRelationListener;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRequirementGroupsListener;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serial;
import java.util.List;

/**
 * Represents a speed group, the special conditions to move units <br>
 * Notice: The requirements mean the unit having this speed group will be able
 * to cross galaxies
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "speed_impact_groups")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners({EntityWithRelationListener.class, EntityWithRequirementGroupsListener.class})
public class SpeedImpactGroup extends EntityWithMissionLimitation<Integer> implements EntityWithRequirementGroups {
    @Serial
    private static final long serialVersionUID = 8120163868349636675L;

    @Column(length = 50)
    private String name;

    @Column(name = "is_fixed")
    private Boolean isFixed = false;

    @Column(name = "mission_explore", nullable = false)
    private Double missionExplore = 0D;

    @Column(name = "mission_gather", nullable = false)
    private Double missionGather = 0D;

    @Column(name = "mission_establish_base", nullable = false)
    private Double missionEstablishBase = 0D;

    @Column(name = "mission_attack", nullable = false)
    private Double missionAttack = 0D;

    @Column(name = "mission_conquest", nullable = false)
    private Double missionConquest = 0D;

    @Column(name = "mission_counterattack", nullable = false)
    private Double missionCounterattack = 0D;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    @Getter
    @Setter
    private ImageStore image;

    @Transient
    private ObjectRelation relation;

    @Transient
    private transient List<RequirementGroup> requirementGroups;

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.SPEED_IMPACT_GROUP;
    }
}
