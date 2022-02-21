package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serial;

/**
 * Has the special field secondValue , which represents the whole reason to
 * match ObjectRelation with a Requirement
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "requirements_information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementInformation implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -4898440527789250186L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relation_id")
    private ObjectRelation relation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @Column(name = "second_value")
    private Long secondValue;

    @Column(name = "third_value")
    private Long thirdValue;
}
