/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.repository.TimeSpecialRepository;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class TimeSpecialBo extends AbstractWithImageBo<Integer, TimeSpecial, TimeSpecialDto>
		implements WithNameBo<Integer, TimeSpecial, TimeSpecialDto> {
	static final long serialVersionUID = -2736277577264790898L;

	@Autowired
	private transient TimeSpecialRepository repository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
	 */
	@Override
	public JpaRepository<TimeSpecial, Integer> getRepository() {
		return repository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<TimeSpecialDto> getDtoClass() {
		return TimeSpecialDto.class;
	}

}
