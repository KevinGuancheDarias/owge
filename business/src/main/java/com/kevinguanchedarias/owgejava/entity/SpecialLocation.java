package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serial;

@Entity
@Table(name = "special_locations")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialLocation extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
    @Serial
    private static final long serialVersionUID = -4665366711844492367L;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "galaxy_id")
    private Galaxy galaxy;

    @OneToOne(mappedBy = "specialLocation")
    @Fetch(FetchMode.JOIN)
    @ToString.Exclude
    private Planet assignedPlanet;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "improvement_id")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    private Improvement improvement;

    @Column(name = "cloned_improvements")
    private Boolean clonedImprovements = false;
}
