package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.dto.AllianceJoinRequestDto;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Service
@AllArgsConstructor
public class AllianceJoinRequestBo
        implements BaseBo<Integer, AllianceJoinRequest, AllianceJoinRequestDto>, UserDeleteListener {
    public static final int ALLIANCE_JOIN_REQUEST_DELETE_USER_ORDER = AllianceBo.ALLIANCE_DELETE_USER_ORDER - 1;

    @Serial
    private static final long serialVersionUID = -596625245649965948L;

    private final AllianceJoinRequestRepository repository;

    @Override
    public JpaRepository<AllianceJoinRequest, Integer> getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<AllianceJoinRequestDto> getDtoClass() {
        return AllianceJoinRequestDto.class;
    }

    /**
     * Saves the request to the database <br>
     * <b>Notice: </b> Defines the <i>requestDate</i> property
     *
     * @throws SgtBackendInvalidInputException When:
     *                                         <ul>
     *                                         <li>You are modifying an existing
     *                                         request</li>
     *                                         <li>You already have a join request
     *                                         for this alliance</li>
     *                                         </ul>
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public AllianceJoinRequest save(AllianceJoinRequest allianceJoinRequest) {
        if (allianceJoinRequest.getId() == null) {
            if (repository.existsByUserAndAlliance(allianceJoinRequest.getUser(),
                    allianceJoinRequest.getAlliance())) {
                throw new SgtBackendInvalidInputException("You already have a join request for this alliance");
            }
        } else {
            throw new SgtBackendInvalidInputException("You cannot modify a join request");
        }
        allianceJoinRequest.setRequestDate(LocalDateTime.now(ZoneOffset.UTC));
        return repository.save(allianceJoinRequest);
    }

    /**
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    public List<AllianceJoinRequest> findByUserId(Integer id) {
        return repository.findByUserId(id);
    }

    @Override
    public int order() {
        return ALLIANCE_JOIN_REQUEST_DELETE_USER_ORDER;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        if (user.getAlliance() == null || !user.getAlliance().getOwner().equals(user)) {
            repository.deleteByUser(user);
        }
    }
}
