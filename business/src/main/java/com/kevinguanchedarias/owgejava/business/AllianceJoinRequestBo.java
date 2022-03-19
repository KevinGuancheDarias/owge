package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.AllianceJoinRequestDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Service
@AllArgsConstructor
public class AllianceJoinRequestBo implements BaseBo<Integer, AllianceJoinRequest, AllianceJoinRequestDto> {
    public static final String ALLIANCE_JOIN_REQUEST_CACHE_TAG = "alliance_join_request";

    @Serial
    private static final long serialVersionUID = -596625245649965948L;

    private final AllianceJoinRequestRepository repository;
    private final transient TaggableCacheManager taggableCacheManager;

    @Override
    public JpaRepository<AllianceJoinRequest, Integer> getRepository() {
        return repository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return ALLIANCE_JOIN_REQUEST_CACHE_TAG;
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
    @Override
    public AllianceJoinRequest save(AllianceJoinRequest allianceJoinRequest) {
        if (allianceJoinRequest.getId() == null) {
            allianceJoinRequest.setRequestDate(new Date());
            if (repository.findOneByUserAndAlliance(allianceJoinRequest.getUser(),
                    allianceJoinRequest.getAlliance()) != null) {
                throw new SgtBackendInvalidInputException("You already have a join request for this alliance");
            }
        } else {
            throw new SgtBackendInvalidInputException("You cannot modify a join request");
        }
        return BaseBo.super.save(allianceJoinRequest);
    }

    /**
     * Finds all request for given alliance
     *
     * @param alliance
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public List<AllianceJoinRequest> findByAlliance(Alliance alliance) {
        return repository.findByAlliance(alliance);
    }

    /**
     * Removes all request associated with the given user
     *
     * @param id
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    public void deleteByUser(Number id) {
        repository.deleteByUserId(id);
    }

    /**
     * @param id
     * @return
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    public List<AllianceJoinRequest> findByUserId(Integer id) {
        return repository.findByUserId(id);
    }
}
