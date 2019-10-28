/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import org.springframework.util.ClassUtils;

import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 * This class forces the key to be numeric <br>
 * <b>why?<b> BeanUtils.copyProperties from
 * {@link DtoUtilService#entityFromDto(Class, DtoFromEntity)} is not able to
 * copy the id, because property's descriptor readMethod would solve return
 * value of CommonDto.getId to Object , while CommonEntity.setId() would
 * properly expect a Number That because CommonDto doesn't have a "requirement"
 * on its generic, so internal check
 * {@link ClassUtils#isAssignable(Class, Class)} would return false, even if
 * both are really number instances, because Java interprets K as Object in
 * CommonDto
 *
 * @see <a href=
 *      "https://gist.github.com/KevinGuancheDarias/33895ed79b09f74f4705a51c1eb8e4eb">Related
 *      Github Gist</a>
 * @param <K> Entity used numeric type
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractCommonDtoWithNumericId<K extends Number> extends CommonDto<K> {
	@Override
	public K getId() {
		// Required because super version interprets K as Object, and not as Number,
		// while Sonar would emit an error, in this case, it is a false positive
		return super.getId();
	}

	@Override
	public void setId(K id) {
		// Required because super version interprets K as Object, and not as Number,
		// while Sonar would emit an error, in this case, it is a false positive
		super.setId(id);
	}
}
