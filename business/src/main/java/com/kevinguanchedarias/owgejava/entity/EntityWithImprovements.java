package com.kevinguanchedarias.owgejava.entity;

/**
 * This interface forces the entity to have certain attributes which allows it
 * to have improvements
 *
 * @author Kevin Guanche Darias
 */
public interface EntityWithImprovements<K> extends EntityWithId<K> {
    Improvement getImprovement();

    void setImprovement(Improvement improvement);

    Boolean getClonedImprovements();

    void setClonedImprovements(Boolean clonedImprovements);
}
