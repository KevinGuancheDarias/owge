package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;

@Service
public class SpeedImpactGroupBo implements BaseBo<Integer, SpeedImpactGroup, DtoFromEntity<SpeedImpactGroup>> {
	private static final long serialVersionUID = 1367954885113224567L;

	@Autowired
	private SpeedImpactGroupRepository speedImpactGroupRepository;

	@Override
	public JpaRepository<SpeedImpactGroup, Integer> getRepository() {
		return speedImpactGroupRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<DtoFromEntity<SpeedImpactGroup>> getDtoClass() {
		throw new SgtBackendNotImplementedException("SpeedImpactGroup doesn't have a dto ... for now =/");
	}

}
