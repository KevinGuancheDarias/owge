package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;

/**
 * Due to entity name, in order to avoid confusions and having to manually put
 * java.lang.Object, this class name is ObjectEntity
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "objects")
public class ObjectEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 418080945672588722L;

    @Id
    @Column(name = "description")
    private String code;

    private String repository;

    /**
     * Finds the code as enum, (to avoid having to clone that too many times)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public ObjectEnum findCodeAsEnum() {
        return ObjectEnum.valueOf(getCode());
    }
}