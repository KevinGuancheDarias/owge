/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import org.hibernate.Hibernate;

import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface DtoWithImprovements {
	public ImprovementDto getImprovement();

	public void setImprovement(ImprovementDto improvementDto);

	/**
	 * 
	 * @param <K>
	 * @param entity
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default <K> void dtoFromEntity(EntityWithImprovements<K> entity) {
		if (Hibernate.isInitialized(entity.getImprovement()) && entity.getImprovement() != null) {
			setImprovement(new ImprovementDto());
			getImprovement().dtoFromEntity(entity.getImprovement());
		}
	}
}
