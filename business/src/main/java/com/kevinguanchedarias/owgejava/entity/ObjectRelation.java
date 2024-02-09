package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serial;
import java.util.List;

/**
 * This entity contains the id of the referenced table and the object type id
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "object_relations")
public class ObjectRelation implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -7972319667060686603L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_description")
    @Fetch(FetchMode.JOIN)
    private ObjectEntity object;

    @Column(name = "reference_id")
    private Integer referenceId;

    @OneToMany(mappedBy = "relation", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<RequirementInformation> requirements;
}
