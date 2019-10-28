package com.kevinguanchedarias.owgejava.entity;

/**
 * This interface forces the entity to have certain attributes which allows it
 * to have improvements
 * 
 * @author Kevin Guanche Darias
 *
 */
public interface EntityWithImprovements<K> extends EntityWithId<K> {
	public Improvement getImprovement();

	public void setImprovement(Improvement improvement);

	public Boolean getClonedImprovements();

	public void setClonedImprovements(Boolean clonedImprovements);
}
