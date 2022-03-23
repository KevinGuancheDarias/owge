package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.RequirementGroupListener;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serial;
import java.util.List;

/**
 * Represents a group of conditions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "requirement_group")
@EntityListeners(RequirementGroupListener.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequirementGroup extends EntityWithRelationImp {
    @Serial
    private static final long serialVersionUID = 6503065882978157947L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String name;

    @Transient
    private List<RequirementInformation> requirements;

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.REQUIREMENT_GROUP;
    }

    /**
     * @return the id
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the name
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The requirements for the given requirement group, known and computed in a
     * listener by using the relation property
     *
     * @return the requirements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<RequirementInformation> getRequirements() {
        return requirements;
    }

    /**
     * @param requirements the requirements to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setRequirements(List<RequirementInformation> requirements) {
        this.requirements = requirements;
    }

}
