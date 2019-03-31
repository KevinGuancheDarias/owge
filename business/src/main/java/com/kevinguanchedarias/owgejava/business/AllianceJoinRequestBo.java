/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class AllianceJoinRequestBo implements BaseBo<AllianceJoinRequest> {
	private static final long serialVersionUID = -596625245649965948L;

	@Autowired
	private AllianceJoinRequestRepository repository;

	@Override
	public JpaRepository<AllianceJoinRequest, Number> getRepository() {
		return repository;
	}

	/**
	 * Saves the request to the database <br>
	 * <b>Notice: </b> Defines the <i>requestDate</i> property
	 * 
	 * @throws SgtBackendInvalidInputException
	 *             When:
	 *             <ul>
	 *             <li>You are modifying an existing request</li>
	 *             <li>You already have a join request for this alliance</li>
	 *             </ul>
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<AllianceJoinRequest> findByAlliance(Alliance alliance) {
		return repository.findByAlliance(alliance);
	}

	/**
	 * Removes all request associated with the given user
	 * 
	 * @param id
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteByUser(Number id) {
		repository.deleteByUserId(id);
	}
}
