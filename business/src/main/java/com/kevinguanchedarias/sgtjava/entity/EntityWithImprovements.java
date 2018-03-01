package com.kevinguanchedarias.sgtjava.entity;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

/**
 * This interface forces the entity to have certain attributes which allows it
 * to have improvements
 * 
 * @author Kevin Guanche Darias
 *
 */
public interface EntityWithImprovements extends SimpleIdEntity {
	public Improvement getImprovement();

	public void setImprovement(Improvement improvement);

	public Boolean getClonedImprovements();

	public void setClonedImprovements(Boolean clonedImprovements);
}
