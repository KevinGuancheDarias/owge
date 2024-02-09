package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import jakarta.persistence.*;

import java.io.Serial;
import java.util.List;

@Entity
@Table(name = "galaxies")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Galaxy implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -230625496064517670L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    private String name;
    private Long sectors;
    private Long quadrants;

    @Column(name = "num_planets")
    private Long numPlanets = 20L;

    @Column(name = "order_number")
    private Integer orderNumber;

    @OneToMany(mappedBy = "galaxy")
    @Cascade({CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DELETE})
    @ToString.Exclude
    private List<Planet> planets;

}
