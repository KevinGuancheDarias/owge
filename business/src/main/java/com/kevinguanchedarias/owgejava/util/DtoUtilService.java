package com.kevinguanchedarias.owgejava.util;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DtoUtilService implements Serializable {
	private static final long serialVersionUID = -2451840948119691965L;

	private static final String INSTANTIATION_ERROR = "Could not create a new instance";

	/**
	 * Null safe method to create a pojo
	 * 
	 * @todo In the future find out a way to remove the static thingy, as is hard to
	 *       mock
	 * @return If the <i>entity</i> is null, will return null, else the generated
	 *         dto
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static <E, P extends DtoFromEntity<E>> P staticDtoFromEntity(Class<P> targetDtoClass, E entity) {
		if (entity != null) {
			try {
				P retVal = targetDtoClass.newInstance();
				retVal.dtoFromEntity(entity);
				return retVal;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CommonException(INSTANTIATION_ERROR, e);
			}
		} else {
			return null;
		}
	}

	public static <E, P extends DtoFromEntity<E>> List<P> staticDtosFromEntities(Class<P> dtoClass, List<E> entities) {
		return entities == null
				? null
				: entities.stream().map(entity -> staticDtoFromEntity(dtoClass, entity)).collect(Collectors.toList());
	}

	public <E, P extends DtoFromEntity<E>> P dtoFromEntity(Class<P> targetDtoClass, E entity) {
		return DtoUtilService.staticDtoFromEntity(targetDtoClass, entity);
	}

	/**
	 * Converts an entire array of entities into an array of pojos
	 *
	 * @param targetDtoClass Dto class to use
	 * @param entities       entities array
	 * @return an array of dtos
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <E, P extends DtoFromEntity<E>> List<P> convertEntireArray(Class<P> targetDtoClass, List<E> entities) {
		List<P> retVal = new ArrayList<>();
		entities.forEach(current -> {
			var currentPojo = DtoUtilService.staticDtoFromEntity(targetDtoClass, current);
			retVal.add(currentPojo);
		});
		return retVal;
	}

	/**
	 * Returns the entity created from the dto <br>
	 * <b>NOTICE:</b> For now only primitive types are copied, in the future should
	 * be different
	 *
	 * @todo In the future create a method toEntity() in {@link DtoFromEntity} to
	 * allow custom entity creations
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.7.0
	 */
	public <P extends DtoFromEntity<E>, E> E entityFromDto(Class<E> targetEntityClass, P pojo) {
		try {
			return entityFromDto(targetEntityClass.newInstance(), pojo);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException(INSTANTIATION_ERROR, e);
		}
	}

	/**
	 * Copies DTO properties to an <b>existing entity class</b>
	 * 
	 * @param <P>      Target DTO (guessed from second argument)
	 * @param <E>      Entity type
	 * @param instance Target <b>existing</b> entity
	 * @param dto      Source DTO
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <P extends DtoFromEntity<E>, E> E entityFromDto(E instance, P dto) {
		BeanUtils.copyProperties(dto, instance);
		return instance;
	}
}
