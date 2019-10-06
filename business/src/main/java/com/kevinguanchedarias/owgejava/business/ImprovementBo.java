/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.repository.ImprovementRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class ImprovementBo implements BaseBo<Integer, Improvement, ImprovementDto> {
	private static final long serialVersionUID = 174646136669035809L;

	@Autowired
	private ImprovementRepository repository;

	@Autowired
	private DtoUtilService dtoUtilService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
	 */
	@Override
	public JpaRepository<Improvement, Integer> getRepository() {
		return repository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<ImprovementDto> getDtoClass() {
		return ImprovementDto.class;
	}

	public Improvement createOrUpdateFromDto(EntityWithImprovements<Number> entityWithImprovements,
			ImprovementDto improvementDto) {
		Integer originalId = null;
		if (entityWithImprovements.getImprovement() == null) {
			entityWithImprovements.setImprovement(new Improvement());
		} else {
			originalId = entityWithImprovements.getImprovement().getId();
		}
		Improvement withNewProperties = dtoUtilService.entityFromDto(entityWithImprovements.getImprovement(),
				improvementDto);
		if (originalId != null) {
			withNewProperties.setId(originalId);
		}
		return save(withNewProperties);
	}
}
