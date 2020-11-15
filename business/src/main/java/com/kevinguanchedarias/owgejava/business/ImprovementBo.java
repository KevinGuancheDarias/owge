/**
 *
 */
package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementChangeEnum;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
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

	private static final Logger LOG = Logger.getLogger(ImprovementBo.class);
	private static final Double DEFAULT_STEP = 10D;
	private static final String CACHE_KEY = "improvements_user";

	@Autowired
	private ImprovementRepository repository;

	@Autowired
	private DtoUtilService dtoUtilService;

	@Autowired
	private transient CacheManager cacheManager;

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	private transient BeanFactory beanFactory;

	private transient List<ImprovementSource> improvementSources = new ArrayList<>();

	private transient Map<ImprovementChangeEnum, List<BiConsumer<Integer, Improvement>>> improvementChangeListeners = new EnumMap<>(
			ImprovementChangeEnum.class);

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

	/**
	 * Adds the specified improvement source which are used to calculate the user
	 * improvements
	 *
	 * @param improvementSource
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void addImprovementSource(ImprovementSource improvementSource) {
		improvementSources.add(improvementSource);
	}

	/**
	 * Adds a change listener
	 *
	 * @param change
	 * @param listener
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void addChangeListener(ImprovementChangeEnum change, BiConsumer<Integer, Improvement> listener) {
		if (improvementChangeListeners.get(change) == null) {
			List<BiConsumer<Integer, Improvement>> actions = new ArrayList<>();
			actions.add(listener);
			improvementChangeListeners.put(change, actions);
		} else {
			improvementChangeListeners.get(change).add(listener);
		}
	}

	/**
	 * Finds the user improvements <br>
	 * <b>NOTICE:</b> If sources are not caching, operation may be very very VERY
	 * LOT GOD SAKE expensive
	 *
	 * @param user
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Cacheable(cacheNames = CACHE_KEY, key = "#user.id")
	public GroupedImprovement findUserImprovement(UserStorage user) {
		LOG.debug("Computing improvements for user " + user.getId());
		GroupedImprovement groupedImprovement = improvementSources.stream()
				.map(current -> findFromCacheOrBo(user, current))
				.reduce(new GroupedImprovement(), GroupedImprovement::add);
		groupedImprovement.addMoreMissions(1F);
		return groupedImprovement;
	}

	/**
	 * Clears the cache
	 *
	 * @param user
	 * @param source
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@CacheEvict(cacheNames = CACHE_KEY, key = "#user.id")
	public void clearSourceCache(UserStorage user, ImprovementSource source) {
		String sourceCacheName = findSourceCacheName(user, source);
		Runnable action = () -> {
			LOG.debug("Clearing cache for " + sourceCacheName);
			cacheManager.getCache(CACHE_KEY).evictIfPresent(user.getId());
			cacheManager.getCache(CACHE_KEY).evictIfPresent(sourceCacheName);
		};
		socketIoService.sendMessage(user, "user_improvements_change", () -> {
			action.run();
			return beanFactory.getBean(getClass()).findUserImprovement(user);
		}, action);
	}

	/**
	 * Emits the current user improvements
	 *
	 * @param user
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void emitUserImprovement(UserStorage user) {
		socketIoService.sendMessage(user, "user_improvements_change",
				() -> beanFactory.getBean(getClass()).findUserImprovement(user));
	}

	/**
	 * Triggers a change detection
	 *
	 * @param improvement
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void triggerChange(Integer userId, Improvement improvement) {
		if (improvement.getMorePrimaryResourceProduction() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_PRIMARY_PRODUCTION, userId, improvement);
		}
		if (improvement.getMoreSecondaryResourceProduction() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_SECONDARY_PRODUCTION, userId, improvement);
		}
		if (improvement.getMoreEnergyProduction() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_ENERGY, userId, improvement);
		}
		if (improvement.getMoreChargeCapacity() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_CHARGE, userId, improvement);
		}
		if (improvement.getMoreMisions() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_MISSIONS, userId, improvement);
		}
		if (improvement.getMoreUpgradeResearchSpeed() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_UPGRADE_RESEARCH_SPEED, userId, improvement);
		}
		if (improvement.getMoreUnitBuildSpeed() > 0.0000F) {
			doTrigger(ImprovementChangeEnum.MORE_UNIT_BUILD_SPEED, userId, improvement);
		}
		if (!improvement.getUnitTypesUpgrades().isEmpty()) {
			doTrigger(ImprovementChangeEnum.UNIT_IMPROVEMENTS, userId, improvement);
		}
	}

	/**
	 * Clears all the cache entries for a given source
	 *
	 * @param source
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@SuppressWarnings("unchecked")
	public void clearCacheEntries(ImprovementSource source) {
		Cache cache = cacheManager.getCache(CACHE_KEY);
		if (Map.class.isInstance(cache.getNativeCache())) {
			Map<Object, GroupedImprovement> cacheEntries = (Map<Object, GroupedImprovement>) cache.getNativeCache();
			AtomicInteger count = new AtomicInteger(0);
			cacheEntries.keySet().stream().filter(key -> !Integer.class.isInstance(key)).map(key -> (String) key)
					.filter(key -> key.startsWith(findSourceServiceName(source))).forEach(key -> {
						cache.evict(key);
						count.incrementAndGet();
					});
			LOG.debug("Cleared " + count + " cache entries from " + findSourceServiceName(source));
		} else {
			LOG.warn("Used cache backend, not supported for selective cache clearing, will clear ALL");
			cache.clear();
		}
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

	/**
	 * Multiplies the value of the improvement for the specified number
	 *
	 * @param current
	 * @param count
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ImprovementDto multiplyValues(Improvement current, Integer count) {
		ImprovementDto improvementDto = new ImprovementDto();
		improvementDto.dtoFromEntity(current);
		return multiplyValues(improvementDto, count);
	}

	/**
	 * Multiplies the value of the improvement for the specified number
	 *
	 * @param improvementDto
	 * @param count
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ImprovementDto multiplyValues(ImprovementDto improvementDto, Integer count) {
		improvementDto.setMoreChargeCapacity(improvementDto.getMoreChargeCapacity() * count);
		improvementDto.setMoreEnergyProduction(improvementDto.getMoreEnergyProduction() * count);
		improvementDto.setMoreMisions(improvementDto.getMoreMisions() * count);
		improvementDto.setMorePrimaryResourceProduction(improvementDto.getMorePrimaryResourceProduction() * count);
		improvementDto.setMoreSecondaryResourceProduction(improvementDto.getMoreSecondaryResourceProduction() * count);
		improvementDto.setMoreUnitBuildSpeed(improvementDto.getMoreUnitBuildSpeed() * count);
		improvementDto.setMoreUpgradeResearchSpeed(improvementDto.getMoreUpgradeResearchSpeed() * count);
		improvementDto.getUnitTypesUpgrades().forEach(currentUnitypeImprovement -> currentUnitypeImprovement
				.setValue(currentUnitypeImprovement.getValue() * count));
		return improvementDto;
	}

	/**
	 * Clears the cache for an entire source, only if the given entity has
	 * improvements defined
	 *
	 * @param <K>
	 * @param entityWithImprovements
	 * @param improvementSource
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <K> void clearCacheEntriesIfRequired(EntityWithImprovements<K> entityWithImprovements,
			ImprovementSource improvementSource) {
		if (entityWithImprovements.getImprovement() != null) {
			clearCacheEntries(improvementSource);
		}
	}

	/**
	 * Utility method to find a base value plus a percentage <br>
	 * If base is 4, and percentage is 25 , will do 4 + 4*0.25
	 *
	 * @param base
	 * @param percentage If null will just return base
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public double computePlusPercertage(Long base, Long percentage) {
		return computePlusPercertage((float) base, (float) percentage);
	}

	/**
	 * Utility method to find a base value plus a percentage <br>
	 * If base is 4, and percentage is 25 , will do 4 + 4*0.25
	 *
	 * @param base
	 * @param percentage If null will just return base
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public double computePlusPercertage(Float base, Float percentage) {
		if (percentage == null) {
			LOG.debug("Percentage is null for base " + base);
			return base;
		} else {
			return base + (base * (percentage / 100));
		}
	}

	/**
	 * Calculates by using a STEP, this is intended to imitate the behavior of SGT
	 * classic level upload
	 *
	 * @param base
	 * @param inputPercentage
	 * @return
	 * @author Kevin Guanche Darias
	 * @since 0.8.1
	 */
	public Double computeImprovementValue(double base, double inputPercentage) {
		return computeImprovementValue(base, inputPercentage, true);
	}

	/**
	 *
	 * @param base
	 * @param inputPercentage
	 * @param sum             If true sums, else sustracts
	 * @return
	 * @since 0.9.13
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double computeImprovementValue(double base, double inputPercentage, boolean sum) {
		double retVal = base;
		double step = Double
				.parseDouble(configurationBo.findOrSetDefault("IMPROVEMENT_STEP", DEFAULT_STEP.toString()).getValue());
		double pendingPercentage = inputPercentage;
		while (pendingPercentage > 0) {
			if (pendingPercentage < step) {
				step = pendingPercentage;
			}
			double current = retVal * (step / 100);
			if (sum) {
				retVal += current;
			} else {
				retVal -= current;
			}
			pendingPercentage -= step;
		}
		return retVal;
	}

	/**
	 * Finds the percentage value as a rational number, for example 80% wuld be
	 * returned as 0.8
	 *
	 * @param inputPercentage
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public double findAsRational(Double inputPercentage) {
		return inputPercentage / 100;
	}

	/**
	 * Finds the percentage value as a rational number, for example 80% wuld be
	 * returned as 0.8
	 *
	 * @param inputPercentage
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public float findAsRational(Float inputPercentage) {
		return (float) findAsRational((double) inputPercentage);
	}

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ImprovementSource> getImprovementSources() {
		return improvementSources;
	}

	private GroupedImprovement findFromCacheOrBo(UserStorage user, ImprovementSource improvementSource) {
		String childCacheKey = findSourceCacheName(user, improvementSource);
		ValueWrapper cached = cacheManager.getCache(CACHE_KEY).get(childCacheKey);
		if (cached == null) {
			GroupedImprovement calculated = improvementSource.calculateImprovement(user);
			cacheManager.getCache(CACHE_KEY).put(childCacheKey, calculated);
			return calculated;
		} else {
			return (GroupedImprovement) cached.get();
		}
	}

	private String findSourceServiceName(ImprovementSource improvementSource) {
		String className = improvementSource.getClass().getName();
		int enhancedBySpringPosition = className.indexOf("$$");
		return enhancedBySpringPosition != -1 ? className.substring(0, enhancedBySpringPosition)
				: improvementSource.getClass().getName();

	}

	private String findSourceCacheName(UserStorage user, ImprovementSource improvementSource) {
		return findSourceServiceName(improvementSource) + '/' + user.getId();
	}

	private void doTrigger(ImprovementChangeEnum name, Integer userId, Improvement improvement) {
		List<BiConsumer<Integer, Improvement>> actions = improvementChangeListeners.get(name);
		if (actions != null) {
			actions.forEach(action -> action.accept(userId, improvement));
		}
	}
}
