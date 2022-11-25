package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;

public class ImprovementUnitTypeDto implements DtoFromEntity<ImprovementUnitType> {
    private Integer id;
    private String type;
    private Integer unitTypeId;
    private String unitTypeName;
    private UnitTypeDto unitType;
    private Long value;

    @Override
    public void dtoFromEntity(ImprovementUnitType entity) {
        id = entity.getId();
        type = entity.getType();
        if (entity.getUnitType() != null) {
            unitType = new UnitTypeDto();
            unitType.dtoFromEntity(entity.getUnitType());
        }
        value = entity.getValue();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use the unit type instead
     */
    @Deprecated(since = "0.9.0")
    public Integer getUnitTypeId() {
        return unitTypeId;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use the unit type instead
     */
    @Deprecated(since = "0.9.0")
    public void setUnitTypeId(Integer unitTypeId) {
        this.unitTypeId = unitTypeId;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use the unit type instead
     */
    @Deprecated(since = "0.9.0")
    public String getUnitTypeName() {
        return unitTypeName;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use the unit type instead
     */
    @Deprecated(since = "0.9.0")
    public void setUnitTypeName(String unitTypeName) {
        this.unitTypeName = unitTypeName;
    }

    /**
     * @return the unitType
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public UnitTypeDto getUnitType() {
        return unitType;
    }

    /**
     * @param unitType the unitType to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setUnitType(UnitTypeDto unitType) {
        this.unitType = unitType;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

}
