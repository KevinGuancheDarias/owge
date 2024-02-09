package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.listener.RequirementComplianceListener;
import com.kevinguanchedarias.owgejava.business.timespecial.UnlockableTimeSpecialService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Service
public class ActiveTimeSpecialBo implements
        BaseBo<Long, ActiveTimeSpecial, ActiveTimeSpecialDto>, ImprovementSource, RequirementComplianceListener, UserDeleteListener {
    @Serial
    private static final long serialVersionUID = -3981337002238422272L;

    private static final Logger LOG = Logger.getLogger(ActiveTimeSpecialBo.class);

    @Autowired
    private transient ActiveTimeSpecialRepository repository;

    @Autowired
    @Lazy
    private TimeSpecialBo timeSpecialBo;

    @Autowired
    private ObjectRelationBo objectRelationBo;

    @Autowired
    private transient UserSessionService userSessionService;

    @Autowired
    private ImprovementBo improvementBo;

    @Autowired
    private transient ScheduledTasksManagerService scheduledTasksManagerService;

    @Autowired
    private transient SocketIoService socketIoService;

    @Autowired
    private RequirementBo requirementBo;

    @Autowired
    private transient RuleRepository ruleRepository;

    @Autowired
    private transient ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private transient ObtainedUnitEventEmitter obtainedUnitEventEmitter;

    @Autowired
    private transient UnlockableTimeSpecialService unlockableTimeSpecialService;

    @PostConstruct
    public void init() {
        improvementBo.addImprovementSource(this);
        scheduledTasksManagerService.addHandler("TIME_SPECIAL_EFFECT_END", task -> {
            Long id = resolveTaskId(task);
            deactivate(id);
        });
        scheduledTasksManagerService.addHandler("TIME_SPECIAL_IS_READY", task -> {
            Long id = resolveTaskId(task);
            LOG.debug("Time special becomes ready, deleting from ActiveTimeSpecial entry with id " + id);
            ActiveTimeSpecial forDelete = findById(id);
            if (forDelete != null) {
                repository.delete(forDelete);
                emitTimeSpecialChange(forDelete.getUser());
            }
        });
    }

    /**
     * Finds active specials with the given state
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<ActiveTimeSpecial> findByUserAndState(Integer userId, TimeSpecialStateEnum state) {
        return repository.findByUserIdAndState(userId, state);
    }

    /**
     * Finds one active by its time special id and the target user, will be null, if
     * not active
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public ActiveTimeSpecial findOneByTimeSpecial(Integer timeSpecialId, Integer userId) {
        return onFind(repository.findOneByTimeSpecialIdAndUserId(timeSpecialId, userId).orElse(null));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.7
     */
    @TaggableCacheable(tags = ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":#user.id", keySuffix = "#user.id")
    public List<TimeSpecialDto> findByUserWithCurrentStatus(UserStorage user) {
        List<TimeSpecialDto> unlockeds = timeSpecialBo.toDto(unlockableTimeSpecialService.findUnlocked(user));
        unlockeds.forEach(
                current -> current.setActiveTimeSpecialDto(toDto(findOneByTimeSpecial(current.getId(), user.getId()))));
        return unlockeds;
    }

    /**
     * Deletes all active time specials <br>
     * <b>Has Propagation.MANDATORY as should not be run by controllers, this action
     * is reserved to another service
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByTimeSpecial(TimeSpecial timeSpecial) {
        repository.deleteByTimeSpecialId(timeSpecial.getId());
    }

    /**
     * Activates a TimeSpecial
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Transactional
    public ActiveTimeSpecial activate(Integer timeSpecialId) {
        var timeSpecial = timeSpecialBo.findByIdOrDie(timeSpecialId);
        var relation = objectRelationBo.findOne(ObjectEnum.TIME_SPECIAL,
                timeSpecial.getId());
        var loggedUser = userSessionService.findLoggedIn();
        objectRelationBo.checkIsUnlocked(loggedUser, relation);
        var currentlyActive = findOneByTimeSpecial(timeSpecial.getId(), loggedUser.getId());
        if (currentlyActive == null) {
            var newActive = new ActiveTimeSpecial();
            newActive.setActivationDate(new Date());
            newActive.setExpiringDate(computeExpiringDate(timeSpecial.getDuration()));
            newActive.setState(TimeSpecialStateEnum.ACTIVE);
            definePendingTime(newActive);
            newActive.setTimeSpecial(timeSpecial);
            var user = userSessionService.findLoggedInWithDetails();
            newActive.setUser(user);
            newActive = repository.save(newActive);
            improvementBo.clearSourceCache(user, this);
            var task = new ScheduledTask("TIME_SPECIAL_EFFECT_END", newActive.getId());
            scheduledTasksManagerService.registerEvent(task, timeSpecial.getDuration());
            requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);
            emitTimeSpecialChange(user);
            emitIfActivationAffectingUnits(newActive);
            applicationEventPublisher.publishEvent(newActive);
            return newActive;
        } else {
            LOG.warn("The specified time special, is already active, doing nothing");
            return currentlyActive;
        }
    }

    @Override
    @Transactional
    public void relationLost(UnlockedRelation unlockedRelation) {
        if (ObjectEnum.TIME_SPECIAL.isObject(unlockedRelation.getRelation().getObject())) {
            var timeSpecialId = unlockedRelation.getRelation().getReferenceId();
            repository.findOneByTimeSpecialIdAndUserId(timeSpecialId, unlockedRelation.getUser().getId())
                    .filter(ats -> TimeSpecialStateEnum.ACTIVE.equals(ats.getState()))
                    .map(ActiveTimeSpecial::getId)
                    .ifPresent(this::deactivate);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
     */
    @Override
    public JpaRepository<ActiveTimeSpecial, Long> getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<ActiveTimeSpecialDto> getDtoClass() {
        return ActiveTimeSpecialDto.class;
    }

    @Override
    public ActiveTimeSpecial onFind(ActiveTimeSpecial activeTimeSpecial) {
        definePendingTime(activeTimeSpecial);
        return activeTimeSpecial;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.interfaces.ImprovementSource#
     * calculateImprovement(com.kevinguanchedarias.owgejava.entity.UserStorage)
     */
    @Override
    public GroupedImprovement calculateImprovement(UserStorage user) {
        GroupedImprovement groupedImprovement = new GroupedImprovement();
        findByUserAndState(user.getId(), TimeSpecialStateEnum.ACTIVE)
                .forEach(current -> groupedImprovement.add(current.getTimeSpecial().getImprovement()));
        return groupedImprovement;
    }

    /**
     * Please note, as we are outside of request, we can't get the user for obvious
     * reasons <br>
     * So we have to manually fill the activeTimeSpecialDto prop
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void emitTimeSpecialChange(UserStorage user) {
        socketIoService.sendMessage(user, "time_special_change", () -> findByUserWithCurrentStatus(user));
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        repository.deleteByUser(user);
    }

    private void deactivate(Long id) {
        LOG.debug("Time special effect end" + id);
        var activeTimeSpecial = findById(id);
        if (activeTimeSpecial != null) {
            var task = new ScheduledTask("TIME_SPECIAL_IS_READY", id);
            activeTimeSpecial.setState(TimeSpecialStateEnum.RECHARGE);
            Long rechargeTime = activeTimeSpecial.getTimeSpecial().getRechargeTime();
            activeTimeSpecial.setReadyDate(computeExpiringDate(rechargeTime));
            repository.save(activeTimeSpecial);
            var user = activeTimeSpecial.getUser();
            improvementBo.clearSourceCache(user, this);
            scheduledTasksManagerService.registerEvent(task, rechargeTime);
            requirementBo.triggerTimeSpecialStateChange(user, activeTimeSpecial.getTimeSpecial());
            emitTimeSpecialChange(user);
            emitIfActivationAffectingUnits(activeTimeSpecial);
        } else {
            LOG.debug("ActiveTimeSpecial was deleted outside");
        }
    }

    private void emitIfActivationAffectingUnits(ActiveTimeSpecial activeTimeSpecial) {
        if (ruleRepository.existsByOriginTypeAndOriginIdAndDestinationTypeIn(
                ObjectEnum.TIME_SPECIAL.name(),
                activeTimeSpecial.getTimeSpecial().getId().longValue(),
                List.of(ObjectEnum.UNIT.name(), "UNIT_TYPE")
        )) {
            obtainedUnitEventEmitter.emitObtainedUnits(activeTimeSpecial.getUser());
        }
    }

    private Date computeExpiringDate(Long time) {
        long difference = new Date().getTime() + time * 1000;
        return new Date(difference);
    }

    private void definePendingTime(ActiveTimeSpecial activeTimeSpecial) {
        if (activeTimeSpecial != null) {
            if (activeTimeSpecial.getState() == TimeSpecialStateEnum.ACTIVE) {
                activeTimeSpecial.setPendingTime(activeTimeSpecial.getExpiringDate().getTime() - new Date().getTime());
            } else {
                activeTimeSpecial.setPendingTime(activeTimeSpecial.getReadyDate().getTime() - new Date().getTime());
            }
        }
    }

    private Long resolveTaskId(ScheduledTask task) {
        if (task.getContent() instanceof Double doubleValue) {
            return doubleValue.longValue();
        } else {
            return (Long) task.getContent();
        }
    }
}
