package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.ObjectRelationToObjectRelationListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Table(name = "object_relation__object_relation")
@Entity
@EntityListeners(ObjectRelationToObjectRelationListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectRelationToObjectRelation implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -7645804574908275461L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_relation_id")
    @Fetch(FetchMode.JOIN)
    private ObjectRelation master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slave_relation_id")
    @Fetch(FetchMode.JOIN)
    private ObjectRelation slave;

}
