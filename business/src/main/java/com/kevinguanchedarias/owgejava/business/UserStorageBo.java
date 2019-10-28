package com.kevinguanchedarias.owgejava.business;

import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.NotYourPlanetException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtFactionNotFoundException;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;

/**
 * Operations with user <b>in this universe</b>
 * 
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class UserStorageBo implements BaseBo<Integer, UserStorage, UserStorageDto> {
	private static final long serialVersionUID = 2837362546838035726L;

	public static final String JWT_SECRET_DB_CODE = "JWT_SECRET";

	@Autowired
	private UserStorageRepository userStorageRepository;

	@Autowired
	private FactionBo factionBo;

	@Autowired
	private PlanetBo planetBo;

	@Autowired
	private RequirementBo requirementBo;

	@Autowired
	private UserImprovementBo userImprovementBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private ImprovementUnitTypeBo improvementUnitTypeBo;

	@Autowired
	private AllianceBo allianceBo;

	@Autowired
	private AuthenticationBo authenticationBo;

	@Override
	public JpaRepository<UserStorage, Integer> getRepository() {
		return userStorageRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<UserStorageDto> getDtoClass() {
		return UserStorageDto.class;
	}

	/**
	 * User exists <b>in this universe</b>
	 * 
	 * @param id
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public boolean exists(Integer id) {
		return userStorageRepository.exists(id);
	}

	/**
	 * User exists <b>in this universe</b>
	 * 
	 * @param user Typically comes from a user token
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public boolean exists(UserStorage user) {
		return exists(user.getId());
	}

	/**
	 * Finds the logged in user information ONLY the base one, and from token<br />
	 * Only id, email, and username will be returned, used
	 * findLoggedInWithDetailts() for everything
	 * 
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public UserStorage findLoggedIn() {
		return convertTokenUserToUserStorage(authenticationBo.findTokenUser());
	}

	/**
	 * Returns the logged in user with ALL his details <br />
	 * <b>NOTICE:</b> If required, will update base information (username,email)
	 * 
	 * @param populateTransient Should Compute transient values<br />
	 *                          Recommended if needs the computed real resource
	 *                          generation after improvements parsing
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public UserStorage findLoggedInWithDetails(boolean populateTransient) {
		UserStorage tokenSimpleUser = findLoggedIn();
		UserStorage dbFullUser = findById(tokenSimpleUser.getId());

		if (!tokenSimpleUser.getEmail().equals(dbFullUser.getEmail())
				|| !tokenSimpleUser.getUsername().equals(dbFullUser.getUsername())) {
			dbFullUser.setEmail(tokenSimpleUser.getEmail());
			dbFullUser.setUsername(tokenSimpleUser.getUsername());
			save(dbFullUser);
		}
		if (populateTransient) {
			dbFullUser.fillTransientValues();
		}
		return dbFullUser;
	}

	public UserStorage findOneByMission(Mission mission) {
		return userStorageRepository.findOneByMissions(mission);
	}

	/**
	 * Will subscribe logged in user to this universe
	 * 
	 * @param factionId Faction that the user wants to use
	 * @return Success registering the user, if user exists already, it's not a
	 *         success!
	 * @author Kevin Guanche Darias
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean subscribe(Integer factionId) {
		if (!factionBo.exists(factionId)) {
			throw new SgtFactionNotFoundException("La facción escogida NO existe");
		}

		UserStorage user = findLoggedIn();

		if (userStorageRepository.exists(user.getId())) {
			return false;
		}

		Faction selectedFaction = factionBo.findById(factionId);
		Planet selectedPlanet = planetBo.findRandomPlanet(null);
		user.setFaction(selectedFaction);
		user.setHomePlanet(selectedPlanet);
		user.setPrimaryResource(selectedFaction.getInitialPrimaryResource().doubleValue());
		user.setSecondaryResource(selectedFaction.getInitialSecondaryResource().doubleValue());
		user.setEnergy(selectedFaction.getInitialEnergy().doubleValue());
		user.setPrimaryResourceGenerationPerSecond(selectedFaction.getPrimaryResourceProduction().doubleValue());
		user.setSecondaryResourceGenerationPerSecond(selectedFaction.getSecondaryResourceProduction().doubleValue());
		user.setLastAction(new Date());
		user = userStorageRepository.save(user);
		user.setImprovements(userImprovementBo.findUserImprovements(user));

		selectedPlanet.setOwner(user);
		selectedPlanet.setHome(true);

		planetBo.save(selectedPlanet);

		requirementBo.triggerFactionSelection(user);
		requirementBo.triggerHomeGalaxySelection(user);
		return user.getId() > 0;
	}

	public Boolean isOfFaction(Integer factionId, Integer userId) {
		return userStorageRepository.findOneByIdAndFactionId(userId, factionId) != null;
	}

	/**
	 * Will update <b>logged in user</b> resources, based on seconds passed since
	 * last resources update
	 * 
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void triggerResourcesUpdate() {
		UserStorage user = findLoggedInWithDetails(true);
		Date now = new Date();
		Date lastLogin = user.getLastAction();
		user.setPrimaryResource(calculateSum(now, lastLogin, user.getComputedPrimaryResourceGenerationPerSecond(),
				user.getPrimaryResource()));
		user.setSecondaryResource(calculateSum(now, lastLogin, user.getComputedSecondaryResourceGenerationPerSecond(),
				user.getSecondaryResource()));
		user.setLastAction(now);
		save(user);
	}

	public boolean isYourPlanet(Planet planet, UserStorage user) {
		return planet.getOwner() != null && planet.getOwner().getId().equals(user.getId());
	}

	/**
	 * Checks if you own the specified planet
	 *
	 * @param planetId
	 * @throws PlanetNotFoundException when planet doesn't exists
	 * @throws NotYourPlanetException  When you do not own the planet
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkOwnPlanet(Long planetId) {
		Planet planet = planetBo.findById(planetId);
		if (planet == null) {
			throw new PlanetNotFoundException("No such planet, with id " + planetId);
		}
		if (!isYourPlanet(planet, findLoggedIn())) {
			throw new NotYourPlanetException("Yo do not own that planet, buy it in the black market?");
		}
	}

	public void addPointsToUser(UserStorage user, Double points) {
		userStorageRepository.addPointsToUser(user, points);
	}

	public Double findConsumedEnergy(UserStorage user) {
		return obtainedUnitBo.findConsumeEnergyByUser(user);
	}

	public Double findMaxEnergy(UserStorage user) {
		return ObjectUtils.firstNonNull(improvementUnitTypeBo.computeImprovementValue(user.getEnergy(),
				userImprovementBo.findUserImprovements(user).getMoreEnergyProduction()), 0D);
	}

	/**
	 * Returns the available energy of the user <br>
	 * <b>NOTICE: Expensive method </b>
	 * 
	 * @param user
	 * @return
	 * @todo For god's sake create a cache system
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double findAvailableEnergy(UserStorage user) {
		return findMaxEnergy(user) - findConsumedEnergy(user);
	}

	/**
	 * Defines the new alliance for all the users having and old alliance <br>
	 * Usually used to delete an alliance
	 * 
	 * @param oldAlliance
	 * @param newAlliance
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void defineAllianceByAllianceId(Integer oldAlliance, Integer newAlliance) {
		Alliance targetNewAlliance = newAlliance == null ? null : allianceBo.findById(newAlliance);
		userStorageRepository.defineAllianceByAllianceId(allianceBo.findById(oldAlliance), targetNewAlliance);
	}

	/**
	 * @param userId
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void leave(Integer userId) {
		UserStorage userRef = getOne(userId);
		if (allianceBo.isOwnerOfAnAlliance(userId)) {
			throw new SgtBackendInvalidInputException("You can't leave your own alliance");
		}
		userRef.setAlliance(null);
		save(userRef);
	}

	private UserStorage convertTokenUserToUserStorage(TokenUser tokenUser) {
		UserStorage user = new UserStorage();
		user.setId(tokenUser.getId().intValue());
		user.setEmail(tokenUser.getEmail());
		user.setUsername(tokenUser.getUsername());
		return user;
	}

	/**
	 * 
	 * @param now            datetime representing now!
	 * @param lastAction     datetime representing the last time value was update
	 * @param perSecondValue Value to increase each second
	 * @param value          current value
	 * @return the new value for the given resource
	 * @author Kevin Guanche Darias
	 */
	private Double calculateSum(Date now, Date lastAction, Double perSecondValue, Double value) {
		Double retVal = value;
		double difference = (now.getTime() - lastAction.getTime()) / (double) 1000;
		retVal += (difference * perSecondValue);
		return retVal;
	}
}