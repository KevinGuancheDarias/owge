package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Service
public class ActiveTimeSpecialBo implements BaseBo<Long, ActiveTimeSpecial, ActiveTimeSpecialDto>, ImprovementSource {
    @Serial
    private static final long serialVersionUID = -3981337002238422272L;

    private static final Logger LOG = Logger.getLogger(ActiveTimeSpecialBo.class);

    @Autowired
    private transient ActiveTimeSpecialRepository repository;

    @Autowired
    private TimeSpecialBo timeSpecialBo;

    @Autowired
    private ObjectRelationBo objectRelationBo;

    @Autowired
    private UserStorageBo userStorageBo;

    @Autowired
    private ImprovementBo improvementBo;

    @Autowired
    private transient ScheduledTasksManagerService scheduledTasksManagerService;

    @Autowired
    private DtoUtilService dtoUtilService;

    @Autowired
    private transient SocketIoService socketIoService;

    @Autowired
    private RequirementBo requirementBo;

    @PostConstruct
    public void init() {
        improvementBo.addImprovementSource(this);
        scheduledTasksManagerService.addHandler("TIME_SPECIAL_EFFECT_END", task -> {
            Long id = resolveTaskId(task);
            LOG.debug("Time special effect end" + id);
            ActiveTimeSpecial activeTimeSpecial = findById(id);
            if (activeTimeSpecial != null) {
                activeTimeSpecial.setState(TimeSpecialStateEnum.RECHARGE);
                Long rechargeTime = activeTimeSpecial.getTimeSpecial().getRechargeTime();
                activeTimeSpecial.setReadyDate(computeExpiringDate(rechargeTime));
                save(activeTimeSpecial);
                UserStorage user = activeTimeSpecial.getUser();
                improvementBo.clearSourceCache(user, this);
                task.setType("TIME_SPECIAL_IS_READY");
                scheduledTasksManagerService.registerEvent(task, rechargeTime);
                requirementBo.triggerTimeSpecialStateChange(user, activeTimeSpecial.getTimeSpecial());
                emitTimeSpecialChange(user);
            } else {
                LOG.debug(
                        "ActiveTimeSpecial was deleted outside... most probable reason, is admin removed the TimeSpecial");
            }
        });
        scheduledTasksManagerService.addHandler("TIME_SPECIAL_IS_READY", task -> {
            Long id = resolveTaskId(task);
            LOG.debug("Time special becomes ready, deleting from ActiveTimeSpecial entry with id " + id);
            ActiveTimeSpecial forDelete = findById(id);
            if (forDelete != null) {
                delete(forDelete);
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
    public List<TimeSpecialDto> findByUserWithCurrentStatus(UserStorage user) {
        List<TimeSpecialDto> unlockeds = timeSpecialBo.toDto(timeSpecialBo.findUnlocked(user));
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
        TimeSpecial timeSpecial = timeSpecialBo.findByIdOrDie(timeSpecialId);
        ObjectRelation relation = objectRelationBo.findOneByObjectTypeAndReferenceId(ObjectEnum.TIME_SPECIAL,
                timeSpecial.getId());
        UserStorage loggedUser = userStorageBo.findLoggedIn();
        objectRelationBo.checkIsUnlocked(loggedUser, relation);
        ActiveTimeSpecial currentlyActive = findOneByTimeSpecial(timeSpecial.getId(), loggedUser.getId());
        if (currentlyActive == null) {
            ActiveTimeSpecial newActive = new ActiveTimeSpecial();
            newActive.setActivationDate(new Date());
            newActive.setExpiringDate(computeExpiringDate(timeSpecial.getDuration()));
            newActive.setState(TimeSpecialStateEnum.ACTIVE);
            definePendingTime(newActive);
            newActive.setTimeSpecial(timeSpecial);
            UserStorage user = userStorageBo.findLoggedInWithDetails();
            newActive.setUser(user);
            newActive = save(newActive);
            improvementBo.clearSourceCache(user, this);
            ScheduledTask task = new ScheduledTask("TIME_SPECIAL_EFFECT_END", newActive.getId());
            scheduledTasksManagerService.registerEvent(task, timeSpecial.getDuration());
            requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);
            emitTimeSpecialChange(user);
            return newActive;
        } else {
            LOG.warn("The specified time special, is already active, doing nothing");
            return currentlyActive;
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
