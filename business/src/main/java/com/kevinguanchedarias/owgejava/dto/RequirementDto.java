package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Requirement;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDto implements WithDtoFromEntityTrait<Requirement> {
    private Integer id;
    private String code;
    private String description;

    /**
     * @return the id
     * @since 0.8.0
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @since 0.8.0
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the code
     * @since 0.8.0
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     * @since 0.8.0
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the description
     * @since 0.8.0
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     * @since 0.8.0
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
