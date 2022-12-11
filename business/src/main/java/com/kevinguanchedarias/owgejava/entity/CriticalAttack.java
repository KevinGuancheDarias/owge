package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serial;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CriticalAttack implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 3178807755504286309L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(length = 100)
    private String name;

    @OneToMany(mappedBy = "criticalAttack")

    @ToString.Exclude
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<CriticalAttackEntry> entries;

}
