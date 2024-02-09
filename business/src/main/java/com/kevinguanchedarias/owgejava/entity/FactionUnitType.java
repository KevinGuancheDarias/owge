package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;

/**
 * Allows to apply faction overrides to specified unit type
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 */
@Entity
@Table(name = "factions_unit_types")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FactionUnitType implements Serializable {
    @Serial
    private static final long serialVersionUID = 3368581537329653944L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction_id")
    private Faction faction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_type_id")
    private UnitType unitType;

    @Column(name = "max_count")
    private Long maxCount;
}
