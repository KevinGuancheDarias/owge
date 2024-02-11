package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.exception.CommonException;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface DtoFromEntity<E> {
    void dtoFromEntity(E entity);

    /**
     * Converts a list of entities into a list of DTOs
     *
     * @param targetDtoClass target DTO class, the passed class MUST implement
     *                       DtoFromEntity interface
     * @param entities       List of entities
     * @return List of specified DTO class
     * @author Kevin Guanche Darias
     */
    default <D extends DtoFromEntity<E>> List<D> dtoFromEntity(Class<D> targetDtoClass, List<E> entities) {
        List<D> retVal = new ArrayList<>();
        entities.stream().forEach(current -> {
            try {
                D currentDto = targetDtoClass.newInstance();
                currentDto.dtoFromEntity(current);
                retVal.add(currentDto);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new CommonException("Error building DTO list from Entity list", e);
            }
        });
        return retVal;
    }
}
