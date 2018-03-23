package com.kevinguanchedarias.sgtjava.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.dto.DtoFromEntity;
import com.kevinguanchedarias.sgtjava.exception.CommonException;

@Service
public class DtoUtilService implements Serializable {
	private static final long serialVersionUID = -2451840948119691965L;

	private static final String INSTANTIATION_ERROR = "Could not create a new instance";

	/**
	 * Null safe method to create a pojo
	 * 
	 * @todo In the future find out a way to remove the static thingy, as is
	 *       hard to mock
	 * @param targetDtoClass
	 * @param entity
	 * @return If the <i>entity</i> is null, will return null, else the
	 *         generated dto
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

	public <E, P extends DtoFromEntity<E>> P dtoFromEntity(Class<P> targetDtoClass, E entity) {
		return DtoUtilService.staticDtoFromEntity(targetDtoClass, entity);
	}

	/**
	 * Converts an entire array of entities into an array of pojos
	 * 
	 * @param targetDtoClass
	 *            Dto class to use
	 * @param entities
	 *            entities array
	 * @return an array of dtos
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <E, P extends DtoFromEntity<E>> List<P> convertEntireArray(Class<P> targetDtoClass, List<E> entities) {
		List<P> retVal = new ArrayList<>();
		entities.forEach(current -> {
			P currentPojo = DtoUtilService.staticDtoFromEntity(targetDtoClass, current);
			retVal.add(currentPojo);
		});
		return retVal;
	}
}
