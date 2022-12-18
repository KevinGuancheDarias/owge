package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.AttackRule;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public class AttackRuleDto implements DtoFromEntity<AttackRule> {
    private Integer id;
    private String name;
    private List<AttackRuleEntryDto> entries;

    @Override
    public void dtoFromEntity(AttackRule entity) {
        loadBaseData(entity);
        if (entity.getAttackRuleEntries() != null) {
            entries = entity.getAttackRuleEntries().stream().map(entry -> {
                var dto = new AttackRuleEntryDto();
                dto.dtoFromEntity(entry);
                return dto;
            }).toList();
        } else {
            entries = new ArrayList<>();
        }
    }

    /**
     * @return the id
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
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
     * @return the entries
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<AttackRuleEntryDto> getEntries() {
        return entries;
    }

    /**
     * @param entries the entries to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setEntries(List<AttackRuleEntryDto> entries) {
        this.entries = entries;
    }

    private void loadBaseData(AttackRule entity) {
        id = entity.getId();
        name = entity.getName();
    }
}
