package com.kevinguanchedarias.owgejava.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * Represents an entity with the common parameters:
 *
 * <ul>
 * <li>id</li>
 * <li>name</li>
 * <li>description</li>
 * </ul>
 *
 * @param <K> Id type
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@MappedSuperclass
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class CommonEntity<K extends Serializable> implements EntityWithId<K> {
    @Serial
    private static final long serialVersionUID = -5044252651188741213L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private K id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = true)
    private String description;

    @Override
    public K getId() {
        return id;
    }

    @Override
    public void setId(K id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
