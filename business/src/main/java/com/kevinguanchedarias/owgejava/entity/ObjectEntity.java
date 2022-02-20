package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
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
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "objects")
public class ObjectEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 418080945672588722L;

    @Id
    private String description;

    @Transient
    private String code;

    private String repository;

    /**
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated To avoid confusions use {@link ObjectEntity#getCode()}
     */
    @Deprecated(since = "0.8.0")
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated To avoid confusions use {@link ObjectEntity#setCode(String)}
     */
    @Deprecated(since = "0.8.0")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the code
     * @todo In the future remove <i>description</i>, and set code as the id, and
     * return it instead of returning <i>description</i>
     * @since 0.8.0
     */
    public String getCode() {
        return description;
    }

    /**
     * @param code the code to set
     * @since 0.8.0
     */
    public void setCode(String code) {
        this.description = code;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String entity) {
        this.repository = entity;
    }

    /**
     * Finds the code as enum, (to avoid having to clone that too many times)
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public ObjectEnum findCodeAsEnum() {
        return ObjectEnum.valueOf(getCode());
    }
}