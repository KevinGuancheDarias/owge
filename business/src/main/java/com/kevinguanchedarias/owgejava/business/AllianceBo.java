package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import com.kevinguanchedarias.owgejava.repository.AllianceRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@AllArgsConstructor
@Service
public class AllianceBo implements WithNameBo<Integer, Alliance, AllianceDto> {
    @Serial
    private static final long serialVersionUID = 2632768998010477053L;

    private final AllianceRepository repository;
    private final ConfigurationBo configurationBo;
    private final UserStorageRepository userStorageRepository;
    private final AuditBo auditBo;
    private final AllianceJoinRequestRepository allianceJoinRequestRepository;

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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @Transactional
    public void delete(Alliance alliance) {
        defineAllianceByAllianceId(alliance.getId());
        repository.delete(alliance);
    }

    /**
     * Saves an alliance to the database <br>
     * <b>NOTICE:</b> Also sets the user alliance of the owner to the <i>newly</i>
     * created alliance
     *
     * @param invokerId User requesting the save
     * @throws ProgrammingException When the owner is null
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @Transactional
    public Alliance save(Alliance alliance, Integer invokerId) {
        if (Boolean.parseBoolean(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE").getValue())) {
            throw new SgtBackendInvalidInputException(
                    "You can't not create an alliance, while the idea is nice, it's not possible");
        }
        Alliance retVal;
        if (alliance.getId() == null) {
            var creator = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, invokerId);
            if (creator.getAlliance() != null) {
                throw new SgtBackendInvalidInputException("You already have an alliance, leave it first");
            }
            alliance.setOwner(creator);
            retVal = repository.save(alliance);
            retVal.getOwner().setAlliance(retVal);
            auditBo.doAudit(AuditActionEnum.JOIN_ALLIANCE);
            userStorageRepository.save(retVal.getOwner());
        } else {
            var storedAlliance = findById(alliance.getId());
            checkInvokerIsOwner(storedAlliance, invokerId);
            storedAlliance.setName(alliance.getName());
            storedAlliance.setDescription(alliance.getDescription());
            retVal = repository.save(storedAlliance);
        }
        return retVal;
    }

    /**
     * @return True if:
     * <ul>
     *   <li>The <i>source</i> user has not an alliance</li>
     *   <li>The <i>target</i>> user has not an alliance</li>
     *   <li>The source alliance is different than the target alliance</li>
     *   </ul>
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public boolean areEnemies(UserStorage source, UserStorage target) {
        return !source.getId().equals(target.getId()) && (source.getAlliance() == null || target.getAlliance() == null
                || !source.getAlliance().getId().equals(target.getAlliance().getId()));
    }

    /**
     * Request the entrance in an alliance
     *
     * @return Persisted user
     * @throws SgtBackendInvalidInputException When you already belong to an
     *                                         alliance
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public AllianceJoinRequest requestJoin(Integer allianceId, Integer invokerId) {
        var alliance = findByIdOrDie(allianceId);
        var user = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, invokerId);
        if (user.getAlliance() != null) {
            throw new SgtBackendInvalidInputException("You are already in an alliance, nice try!");
        }
        var retVal = new AllianceJoinRequest();
        retVal.setAlliance(alliance);
        retVal.setUser(user);
        return allianceJoinRequestRepository.save(AllianceJoinRequest.builder()
                .alliance(alliance)
                .user(user)
                .build()
        );
    }

    @Transactional
    public void acceptJoin(Integer joinRequestId, Number invoker) {
        var request = SpringRepositoryUtil.findByIdOrDie(allianceJoinRequestRepository, joinRequestId);
        checkInvokerIsOwner(request.getAlliance(), invoker);
        checkIsLimitReached(request);
        if (request.getUser().getAlliance() == null) {
            var alliance = request.getAlliance();
            request.getUser().setAlliance(alliance);
            alliance.getUsers()
                    .forEach(user -> auditBo.nonRequestAudit(AuditActionEnum.USER_INTERACTION, "JOIN_ALLIANCE", request.getUser(), user.getId()));
            auditBo.doAudit(AuditActionEnum.ACCEPT_JOIN_ALLIANCE, null, request.getUser().getId());
            userStorageRepository.save(request.getUser());
            allianceJoinRequestRepository.deleteByUser(request.getUser());
        } else {
            allianceJoinRequestRepository.delete(request);
        }
    }

    /**
     * Rejects the join request
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public void rejectJoin(Integer joinRequestId, Number invoker) {
        var request = SpringRepositoryUtil.findByIdOrDie(allianceJoinRequestRepository, joinRequestId);
        checkInvokerIsOwner(request.getAlliance(), invoker);
        allianceJoinRequestRepository.delete(request);
    }

    /**
     * Leaves an alliance
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @Transactional
    public void leave(Integer userId) {
        var userRef = userStorageRepository.getReferenceById(userId);
        if (isOwnerOfAnAlliance(userId)) {
            throw new SgtBackendInvalidInputException("You can't leave your own alliance");
        }
        userRef.setAlliance(null);
        userStorageRepository.save(userRef);
    }

    /**
     * Deletes the alliance associated with the user
     *
     * @throws SgtBackendInvalidInputException When user is not the owner of the
     *                                         alliance
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @Transactional
    public void deleteByUser(UserStorage transientUser) {
        var user = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, transientUser.getId());
        var alliance = user.getAlliance();
        if (alliance == null) {
            throw new SgtBackendInvalidInputException("You don't have any alliance");
        }
        checkInvokerIsOwner(alliance, user);
        delete(alliance);
    }

    /**
     * Checks if the user is the owner of the request
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public void checkInvokerIsOwner(Alliance storedAlliance, UserStorage user) {
        checkInvokerIsOwner(storedAlliance, user.getId());
    }

    /**
     * Checks if the alliance is of user property
     *
     * @throws SgtBackendInvalidInputException When user is not the owner of the
     *                                         alliance
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public boolean isOwnerOfAnAlliance(Number userId) {
        return repository.findOneByOwnerId(userId) != null;
    }

    private void checkIsLimitReached(AllianceJoinRequest request) {
        long userCount = userStorageRepository.count();
        float allowedPercentage = Integer
                .parseInt(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE_PERCENTAGE", "7").getValue());
        var max = Integer.parseInt(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE", "15").getValue());
        float allowedByPercentage = userCount * (allowedPercentage / 100);
        allowedByPercentage = allowedByPercentage < 2 ? 2 : allowedByPercentage;
        int maxAllowed = (int) (allowedByPercentage < max ? allowedByPercentage : max);
        if (maxAllowed <= userStorageRepository.countByAlliance(request.getAlliance())) {
            throw new SgtBackendInvalidInputException("I18N_ERR_ALLIANCE_IS_FULL");
        }

    }

    /**
     * Defines the new alliance for all the users having and old alliance <br>
     * Usually used to delete an alliance
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    private void defineAllianceByAllianceId(Integer oldAlliance) {
        userStorageRepository.defineAllianceByAllianceId(findById(oldAlliance), null);
    }
}
