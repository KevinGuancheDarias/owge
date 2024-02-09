package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * Represents the capability that a {@link Unit} may have to intercept other
 * units having the desired {@link SpeedImpactGroup}
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 */
@Entity
@Table(name = "interceptable_speed_group")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterceptableSpeedGroup implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 2487571740734931586L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speed_impact_group_id")
    private SpeedImpactGroup speedImpactGroup;

}
