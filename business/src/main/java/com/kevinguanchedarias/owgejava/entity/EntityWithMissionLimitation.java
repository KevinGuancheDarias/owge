package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serial;
import java.io.Serializable;

@MappedSuperclass
@Data
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public abstract class EntityWithMissionLimitation<K extends Serializable> implements EntityWithId<K> {
    @Serial
    private static final long serialVersionUID = 3396208017507627247L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private K id;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_explore", nullable = false)
    @Builder.Default
    private MissionSupportEnum canExplore = MissionSupportEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_gather", nullable = false)
    @Builder.Default
    private MissionSupportEnum canGather = MissionSupportEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_establish_base", nullable = false)
    @Builder.Default
    private MissionSupportEnum canEstablishBase = MissionSupportEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_attack", nullable = false)
    @Builder.Default
    private MissionSupportEnum canAttack = MissionSupportEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_counterattack", nullable = false)
    @Builder.Default
    private MissionSupportEnum canCounterattack = MissionSupportEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_conquest", nullable = false)
    @Builder.Default
    private MissionSupportEnum canConquest = MissionSupportEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_deploy", nullable = false)
    @Builder.Default
    private MissionSupportEnum canDeploy = MissionSupportEnum.ANY;
}
