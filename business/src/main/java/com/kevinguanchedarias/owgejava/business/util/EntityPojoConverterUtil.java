package com.kevinguanchedarias.owgejava.business.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
@Slf4j
public class EntityPojoConverterUtil {
    private static final String COMMON_ERROR = "Unable to create an instance of {}";

    /**
     * Copies object from one class to other.<br />
     * <b>NOTICE:</b> Only name marching properties are copied
     *
     * @param targetClass - Class of the result conversion
     * @param source      Object to copy props from
     * @return Result object of the copy with the specified class
     * @author Kevin Guanche Darias
     */
    public static <E> E convertFromTo(Class<E> targetClass, Object source) {
        E targetEntity = null;
        try {
            targetEntity = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, targetEntity);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            log.error(COMMON_ERROR, targetClass, e);
        }
        return targetEntity;
    }

    /**
     * Copies object from one class to one existing instance
     *
     * @param targetEntity - Instance of target entity
     * @author Kevin Guanche Darias
     */
    public static <E> E convertFromTo(E targetEntity, Object source) {
        BeanUtils.copyProperties(source, targetEntity);
        return targetEntity;
    }

    /**
     * Copies object from one class to other.<br />
     * <b>NOTICE:</b> Only name marching properties are copied
     *
     * @param targetClass - Class of the result conversion
     * @return Result list of the copy with the specified class
     * @author Kevin Guanche Darias
     */
    public static <E> List<E> convertFromTo(Class<E> targetClass, List<?> sourceList) {
        List<E> retVal = null;
        try {
            retVal = new ArrayList<>();
            for (Object current : sourceList) {
                E targetObject = targetClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(current, retVal);
                retVal.add(targetObject);
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            log.error(COMMON_ERROR, targetClass, e);
        }
        return retVal;
    }
}
