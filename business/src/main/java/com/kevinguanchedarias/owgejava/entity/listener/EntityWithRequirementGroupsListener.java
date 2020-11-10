package com.kevinguanchedarias.owgejava.entity.listener;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.PostLoad;

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationToObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Component
public class EntityWithRequirementGroupsListener {
	private static final String CACHE_KEY = "requirement_groups_listener";

	private ObjectRelationBo objectRelationBo;
	private ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo;
	private CacheManager cacheManager;

	@Lazy
	public EntityWithRequirementGroupsListener(ObjectRelationBo objectRelationBo,
			ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo, CacheManager cacheManager) {
		this.objectRelationBo = objectRelationBo;
		this.objectRelationToObjectRelationBo = objectRelationToObjectRelationBo;
		this.cacheManager = cacheManager;
	}

	/**
	 *
	 * @param entityWithGroupRequirements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@SuppressWarnings("unchecked")
	@PostLoad
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void loadRequirements(EntityWithRequirementGroups entityWithGroupRequirements) {
		List<RequirementGroup> requirementGroups;
		Integer key = entityWithGroupRequirements.getRelation().getId();
		Cache cacheStore = cacheManager.getCache(CACHE_KEY);
		if (cacheStore != null) {
			ValueWrapper cached = cacheStore.get(key);
			if (cached == null || cached.get() == null) {
				requirementGroups = loadRequirementGroups(key);
				cacheStore.put(key, requirementGroups);
			} else {
				requirementGroups = (List<RequirementGroup>) cached.get();
			}
		} else {
			requirementGroups = loadRequirementGroups(key);
		}
		entityWithGroupRequirements.setRequirementGroups(requirementGroups);
	}

	private List<RequirementGroup> loadRequirementGroups(Integer key) {
		return objectRelationToObjectRelationBo.findByMasterId(key).stream()
				.map(relationToRelation -> (RequirementGroup) objectRelationBo
						.unboxObjectRelation(relationToRelation.getSlave()))
				.collect(Collectors.toList());
	}
}
