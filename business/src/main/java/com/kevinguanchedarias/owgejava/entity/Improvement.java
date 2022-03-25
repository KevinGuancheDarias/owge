package com.kevinguanchedarias.owgejava.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serial;
import java.util.List;

@Entity
@Table(name = "improvements")
@Data
@EqualsAndHashCode(callSuper = true)
public class Improvement extends ImprovementBase<Integer> {
    @Serial
    private static final long serialVersionUID = 2210759653999657847L;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "improvementId")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    @Fetch(FetchMode.JOIN)
    private List<ImprovementUnitType> unitTypesUpgrades;
}
