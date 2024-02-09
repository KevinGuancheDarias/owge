package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serial;
import java.util.List;

@Entity
@Table(name = "improvements")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Improvement extends ImprovementBase<Integer> {
    @Serial
    private static final long serialVersionUID = 2210759653999657847L;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "improvementId")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    @Fetch(FetchMode.JOIN)
    @ToString.Exclude
    private List<ImprovementUnitType> unitTypesUpgrades;
}
