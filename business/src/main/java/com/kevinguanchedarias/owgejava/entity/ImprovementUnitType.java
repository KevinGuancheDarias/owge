package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "improvements_unit_types")
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementUnitType implements EntityWithId<Integer> {

    @Serial
    private static final long serialVersionUID = -6385439199243097164L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "improvement_id")
    private Improvement improvementId;

    @Column(name = "type")
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "unit_type_id")
    private UnitType unitType;

    private Long value;
}
