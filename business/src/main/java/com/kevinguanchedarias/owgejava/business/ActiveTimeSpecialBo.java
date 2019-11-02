/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.OwgeSqsMessageEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.pojo.OwgeSqsMessage;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class ActiveTimeSpecialBo implements BaseBo<Long, ActiveTimeSpecial, ActiveTimeSpecialDto> {
	/**
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
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
	private transient SqsManagerService sqsManagerService;

	@Autowired
	private DtoUtilService dtoUtilService;

	@PostConstruct
	public void init() {
		sqsManagerService.addHandler(OwgeSqsMessageEnum.TIME_SPECIAL_EFFECT_END, message -> {
			LOG.debug("Time special effect end" + message.findSimpleContent());
			ActiveTimeSpecial activeTimeSpecial = findById(Long.valueOf((Integer) message.findSimpleContent()));
			activeTimeSpecial.setState(TimeSpecialStateEnum.RECHARGE);
			Long rechargeTime = activeTimeSpecial.getTimeSpecial().getRechargeTime();
			activeTimeSpecial.setExpiringDate(computeExpiringDate(rechargeTime));
			save(activeTimeSpecial);
			sqsManagerService.sendMessage(
					new OwgeSqsMessage(OwgeSqsMessageEnum.TIME_SPECIAL_IS_READY, activeTimeSpecial.getId()),
					rechargeTime);
		});
		sqsManagerService.addHandler(OwgeSqsMessageEnum.TIME_SPECIAL_IS_READY, message -> {
			Long id = Long.valueOf((Integer) message.findSimpleContent());
			LOG.debug("Time special becomes ready, deleting from ActiveTimeSpecial entry with id " + id);
			delete(id);
		});
	}

	/**
	 * Find by user
	 * 
	 * @param userId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ActiveTimeSpecial> findByUser(Integer userId) {
		return repository.findByUserId(userId).stream().map(this::onFind).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param userId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ActiveTimeSpecialDto> findByUserAsDto(Integer userId) {
		return dtoUtilService.convertEntireArray(ActiveTimeSpecialDto.class, findByUser(userId));
	}

	/**
	 * Finds one active by its time special id, will be null, if not active
	 * 
	 * @param timeSpecialId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ActiveTimeSpecial findOneByTimeSpecial(Integer timeSpecialId) {
		return repository.findOneByTimeSpecialId(timeSpecialId);
	}

	/**
	 * Activates a TimeSpecial
	 * 
	 * @param timeSpecialId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public ActiveTimeSpecial activate(Integer timeSpecialId) {
		TimeSpecial timeSpecial = timeSpecialBo.findByIdOrDie(timeSpecialId);
		ObjectRelation relation = objectRelationBo.findOneByObjectTypeAndReferenceId(ObjectEnum.TIME_SPECIAL,
				timeSpecial.getId());
		objectRelationBo.checkIsUnlocked(userStorageBo.findLoggedIn(), relation);
		ActiveTimeSpecial currentlyActive = findOneByTimeSpecial(timeSpecial.getId());
		if (currentlyActive == null) {
			ActiveTimeSpecial newActive = new ActiveTimeSpecial();
			newActive.setActivationDate(new Date());
			newActive.setExpiringDate(computeExpiringDate(timeSpecial.getDuration()));
			newActive.setTimeSpecial(timeSpecial);
			newActive.setUser(userStorageBo.findLoggedInWithDetails(false));
			newActive.setState(TimeSpecialStateEnum.ACTIVE);
			newActive = save(newActive);
			sqsManagerService.sendMessage(
					new OwgeSqsMessage(OwgeSqsMessageEnum.TIME_SPECIAL_EFFECT_END, newActive.getId()),
					timeSpecial.getDuration());
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
		activeTimeSpecial.setPendingTime(activeTimeSpecial.getExpiringDate().getTime() - new Date().getTime());
		return activeTimeSpecial;
	}

	private Date computeExpiringDate(Long time) {
		Long difference = new Date().getTime() + time * 1000;
		return new Date(difference);
	}
}
