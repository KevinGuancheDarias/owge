package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.Alliance;
import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;
import com.kevinguanchedarias.sgtjava.repository.AllianceRepository;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class AllianceBo implements WithNameBo<Alliance> {
	private static final long serialVersionUID = 2632768998010477053L;

	@Autowired
	private AllianceRepository repository;

	@Autowired
	private UserStorageBo userStorageBo;

	/**
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public JpaRepository<Alliance, Number> getRepository() {
		return repository;
	}

	/**
	 * Deletes an alliance
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

	/**
	 * Saves an alliance to the database <br>
	 * <b>NOTICE:</b> Also sets the user alliance of the owner to the
	 * <i>newly</i> created alliance
	 * 
	 * @param alliance
	 * @throws ProgrammingException
	 *             When the owner is null
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Alliance save(Alliance alliance) {
		if (alliance.getOwner() == null) {
			throw new ProgrammingException("An alliance MUST have an owner before saving it");
		}
		Alliance retVal;
		if (alliance.getId() == null) {
			retVal = WithNameBo.super.save(alliance);
			retVal.getOwner().setAlliance(retVal);
			userStorageBo.save(retVal.getOwner());
		} else {
			retVal = WithNameBo.super.save(alliance);
		}
		return retVal;
	}
}
