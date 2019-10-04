package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TimeSpecialBo;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.rest.AbstractCrudFullRestService;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@Scope()
@RequestMapping("admin/time_special")
public class AdminTimeSpecialRestService
		extends AbstractCrudFullRestService<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> {

	@Autowired
	private TimeSpecialBo timeSpecialBo;

	@Override
	public Class<TimeSpecial> getEntityClass() {
		return TimeSpecial.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.AdminCrudRestServiceTrait#
	 * getBo()
	 */
	@Override
	public TimeSpecialBo getBo() {
		return timeSpecialBo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.AdminCrudRestServiceTrait#
	 * getDtoClass()
	 */
	@Override
	public Class<TimeSpecialDto> getDtoClass() {
		return TimeSpecialDto.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceNoOpEventsTrait#
	 * getSupportedOperationsBuilder()
	 */
	@Override
	public SupportedOperationsBuilder getSupportedOperationsBuilder() {
		return SupportedOperationsBuilder.create().withFullPrivilege();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getObject()
	 */
	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.TIME_SPECIAL;
	}

}
