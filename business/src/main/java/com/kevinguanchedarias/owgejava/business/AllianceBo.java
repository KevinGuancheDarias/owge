package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AllianceRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@AllArgsConstructor
@Service
public class AllianceBo implements WithNameBo<Integer, Alliance, AllianceDto> {
	private static final long serialVersionUID = 2632768998010477053L;

	private final AllianceRepository repository;
	private final UserStorageBo userStorageBo;
	private final AllianceJoinRequestBo allianceJoinRequestBo;
	private final ConfigurationBo configurationBo;
	private final AuditBo auditBo;

	/**
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.7.0
	 */
	@Override
	public JpaRepository<Alliance, Integer> getRepository() {
		return repository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<AllianceDto> getDtoClass() {
		return AllianceDto.class;
	}

	/**
	 * Deletes an alliance <br>
	 * <b>NOTICE: </b> Handles unsetting all users
	 *
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	@Transactional
	public void delete(Alliance alliance) {
		userStorageBo.defineAllianceByAllianceId(alliance.getId(), null);
		WithNameBo.super.delete(alliance);
	}

	@Override
	public Alliance save(Alliance alliance) {
		throw new ProgrammingException("Invoker user is always required to save an alliance");
	}

	/**
	 * Saves an alliance to the database <br>
	 * <b>NOTICE:</b> Also sets the user alliance of the owner to the <i>newly</i>
	 * created alliance
	 *
	 * @param invokerId User requesting the save
	 * @throws ProgrammingException When the owner is null
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public Alliance save(Alliance alliance, Integer invokerId) {
		if (Boolean.parseBoolean(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE").getValue())) {
			throw new SgtBackendInvalidInputException(
					"You can't not create an alliance, while the idea is nice, it's not possible");
		}
		Alliance retVal;
		if (alliance.getId() == null) {
			var creator = userStorageBo.findById(invokerId);
			if (creator.getAlliance() != null) {
				throw new SgtBackendInvalidInputException("You already have an alliance, leave it first");
			}
			alliance.setOwner(creator);
			retVal = WithNameBo.super.save(alliance);
			retVal.getOwner().setAlliance(retVal);
			auditBo.doAudit(AuditActionEnum.JOIN_ALLIANCE);
			userStorageBo.save(retVal.getOwner());
		} else {
			var storedAlliance = findById(alliance.getId());
			checkInvokerIsOwner(storedAlliance, invokerId);
			storedAlliance.setName(alliance.getName());
			storedAlliance.setDescription(alliance.getDescription());
			retVal = WithNameBo.super.save(storedAlliance);
		}
		return retVal;
	}

	/**
	 * Request the entrance in an alliance
	 *
	 * @param allianceId
	 * @param ownerId
	 * @return Persisted user
	 * @since 0.7.0
	 * @throws SgtBackendInvalidInputException When you already belong to an
	 *                                         alliance
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AllianceJoinRequest requestJoin(Integer allianceId, Integer ownerId) {
		Alliance alliance = findByIdOrDie(allianceId);
		UserStorage user = userStorageBo.findByIdOrDie(ownerId);
		if (user.getAlliance() != null) {
			throw new SgtBackendInvalidInputException("You are already in an alliance, nice try!");
		}
		var retVal = new AllianceJoinRequest();
		retVal.setAlliance(alliance);
		retVal.setUser(user);
		return allianceJoinRequestBo.save(retVal);
	}

	@Transactional
	public void acceptJoin(Integer joinRequestId, Number invoker) {
		var request = allianceJoinRequestBo.findByIdOrDie(joinRequestId);
		checkInvokerIsOwner(request.getAlliance(), invoker);
		checkIsLimitReached(request);
		if (request.getUser().getAlliance() == null) {
			var alliance = request.getAlliance();
			request.getUser().setAlliance(alliance);
			alliance.getUsers()
					.forEach(user -> auditBo.nonRequestAudit(AuditActionEnum.USER_INTERACTION, "JOIN_ALLIANCE", request.getUser(), user.getId()));
			auditBo.doAudit(AuditActionEnum.ACCEPT_JOIN_ALLIANCE, null, request.getUser().getId());
			userStorageBo.save(request.getUser());
			allianceJoinRequestBo.deleteByUser(request.getUser().getId());
		} else {
			allianceJoinRequestBo.delete(request);
		}
	}

	/**
	 * Rejects the join request
	 *
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void rejectJoin(Integer joinRequestId, Number invoker) {
		var request = allianceJoinRequestBo.findByIdOrDie(joinRequestId);
		checkInvokerIsOwner(request.getAlliance(), invoker);
		allianceJoinRequestBo.delete(request);
	}

	/**
	 * Leaves an alliance
	 *
	 * @param userId
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void leave(Integer userId) {
		userStorageBo.leave(userId);
	}

	/**
	 *
	 * @param allianceId
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public List<UserStorage> findMembers(Integer allianceId) {
		Alliance alliance = findByIdOrDie(allianceId);
		alliance.getUsers().size();
		return alliance.getUsers();
	}

	/**
	 * Deletes the alliance associated with the user
	 *
	 * @param transientUser
	 * @throws SgtBackendInvalidInputException When user is not the owner of the
	 *                                         alliance
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void delete(UserStorage transientUser) {
		UserStorage user = userStorageBo.findById(transientUser.getId());
		Alliance alliance = user.getAlliance();
		if (alliance == null) {
			throw new SgtBackendInvalidInputException("You don't have any alliance");
		}
		checkInvokerIsOwner(alliance, user);
		delete(alliance);
	}

	/**
	 * Checks if the user is the owner of the request
	 *
	 * @param storedAlliance
	 * @param user
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkInvokerIsOwner(Alliance storedAlliance, UserStorage user) {
		checkInvokerIsOwner(storedAlliance, user.getId());
	}

	/**
	 * Checks if the alliance is of user property
	 *
	 * @param storedAlliance
	 * @param userId
	 * @throws SgtBackendInvalidInputException When user is not the owner of the
	 *                                         alliance
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkInvokerIsOwner(Alliance storedAlliance, Number userId) {
		if (!storedAlliance.getOwner().getId().equals(userId)) {
			throw new SgtBackendInvalidInputException(
					"You are NOT the owner of that alliance, try hacking the owner account");
		}
	}

	/**
	 * Returns true if user has an alliance
	 *
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean isOwnerOfAnAlliance(Number userId) {
		return repository.findOneByOwnerId(userId) != null;
	}

	private void checkIsLimitReached(AllianceJoinRequest request) {
		long userCount = userStorageBo.countAll();
		float allowedPercentage = Integer
				.parseInt(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE_PERCENTAGE", "7").getValue());
		var max = Integer.parseInt(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE", "15").getValue());
		float allowedByPercentage = userCount * (allowedPercentage / 100);
		allowedByPercentage = allowedByPercentage < 2 ? 2 : allowedByPercentage;
		int maxAllowed = (int) (allowedByPercentage < max ? allowedByPercentage : max);
		if (maxAllowed <= userStorageBo.countByAlliance(request.getAlliance())) {
			throw new SgtBackendInvalidInputException("I18N_ERR_ALLIANCE_IS_FULL");
		}

	}
}
