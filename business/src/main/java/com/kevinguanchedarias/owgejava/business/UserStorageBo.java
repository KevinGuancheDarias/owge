package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.NotYourPlanetException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtFactionNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

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
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private AllianceBo allianceBo;

	@Autowired
	private AuthenticationBo authenticationBo;

	@Autowired
	private ImprovementBo improvementBo;

	@Autowired
	private transient EntityManager entityManager;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	private DtoUtilService dtoUtilService;

	@Autowired
	@Lazy
	private AuditBo auditBo;

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
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public boolean exists(Integer id) {
		return userStorageRepository.existsById(id);
	}

	/**
	 * User exists <b>in this universe</b>
	 *
	 * @param user Typically comes from a user token
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
	 * @author Kevin Guanche Darias
	 */
	public UserStorage findLoggedIn() {
		var token = authenticationBo.findTokenUser();
		return token != null ? convertTokenUserToUserStorage(token) : null;
	}

	/**
	 *
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<Integer> findAllIds() {
		return userStorageRepository.findAllIds();
	}

	@Transactional
	public UserStorage findLoggedInWithDetails() {
		UserStorage tokenSimpleUser = findLoggedIn();
		if (tokenSimpleUser != null) {
			UserStorage dbFullUser = findById(tokenSimpleUser.getId());

			if (!tokenSimpleUser.getEmail().equals(dbFullUser.getEmail())
					|| !tokenSimpleUser.getUsername().equals(dbFullUser.getUsername())) {
				dbFullUser.setEmail(tokenSimpleUser.getEmail());
				dbFullUser.setUsername(tokenSimpleUser.getUsername());
				save(dbFullUser);
			}
			return dbFullUser;
		} else {
			return null;
		}
	}

	public UserStorage findOneByMission(Mission mission) {
		return userStorageRepository.findOneByMissions(mission);
	}

	/**
	 *
	 * @since 0.9.14
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public int countByAlliance(Alliance alliance) {
		return userStorageRepository.countByAlliance(alliance);
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

		if (userStorageRepository.existsById(user.getId())) {
			return false;
		}

		var selectedFaction = factionBo.findById(factionId);
		var selectedPlanet = planetBo.findRandomPlanet(null);
		user.setFaction(selectedFaction);
		user.setHomePlanet(selectedPlanet);
		user.setPrimaryResource(selectedFaction.getInitialPrimaryResource().doubleValue());
		user.setSecondaryResource(selectedFaction.getInitialSecondaryResource().doubleValue());
		user.setEnergy(selectedFaction.getInitialEnergy().doubleValue());
		user.setLastAction(new Date());
		user = userStorageRepository.save(user);

		selectedPlanet.setOwner(user);
		selectedPlanet.setHome(true);

		planetBo.save(selectedPlanet);

		requirementBo.triggerFactionSelection(user);
		requirementBo.triggerHomeGalaxySelection(user);
		if (user.getId() > 0) {
			auditBo.doAudit(AuditActionEnum.SUBSCRIBE_TO_WORLD);
		}
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
	public void triggerResourcesUpdate(Integer userId) {
		UserStorage user = findByIdOrDie(userId);
		var faction = user.getFaction();
		GroupedImprovement userImprovements = improvementBo.findUserImprovement(user);
		var now = new Date();
		var lastLogin = user.getLastAction();
		userStorageRepository.addResources(user, now,
				calculateSum(now, lastLogin,
						computeUserResourcePerSecond(faction.getPrimaryResourceProduction(),
								userImprovements.getMorePrimaryResourceProduction())),
				calculateSum(now, lastLogin, computeUserResourcePerSecond(faction.getSecondaryResourceProduction(),
						userImprovements.getMoreSecondaryResourceProduction())));
	}

	public boolean isYourPlanet(Planet planet, UserStorage user) {
		return planet.getOwner() != null && planet.getOwner().getId().equals(user.getId());
	}

	/**
	 * Checks if you own the specified planet
	 *
	 * @throws PlanetNotFoundException when planet doesn't exists
	 * @throws NotYourPlanetException  When you do not own the planet
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkOwnPlanet(Long planetId) {
		var planet = planetBo.findById(planetId);
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

	/**
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double findMaxEnergy(Integer userId) {
		return findMaxEnergy(findByIdOrDie(userId));
	}

	public Double findMaxEnergy(UserStorage user) {
		var groupedImprovement = improvementBo.findUserImprovement(user);
		var faction = user.getFaction();
		return improvementBo.computeImprovementValue(faction.getInitialEnergy().floatValue(),
				groupedImprovement.getMoreEnergyProduction());
	}

	/**
	 * Returns the available energy of the user <br>
	 * <b>NOTICE: Expensive method </b>
	 *
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
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void defineAllianceByAllianceId(Integer oldAlliance, Integer newAlliance) {
		Alliance targetNewAlliance = newAlliance == null ? null : allianceBo.findById(newAlliance);
		userStorageRepository.defineAllianceByAllianceId(allianceBo.findById(oldAlliance), targetNewAlliance);
	}

	/**
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

	/**
	 * Saves the user <br>
	 * <b>NOTICE:</b> Emits the change to all connected websockets
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UserStorage save(UserStorage user, boolean emitChange) {
		UserStorage savedUser = BaseBo.super.save(user);
		if (emitChange && user.getId() != null) {
			TransactionUtil.doAfterCommit(() -> {
				entityManager.refresh(savedUser);
				emitUserData(user);
			});
		}
		return savedUser;
	}

	@Transactional
	@Override
	public UserStorage save(UserStorage user) {
		return save(user, true);
	}

	/**
	 * Finds all the user data as a DTO
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorageDto findData(UserStorage user) {
		var userDto = new UserStorageDto();
		userDto.dtoFromEntity(user);
		userDto.setImprovements(improvementBo.findUserImprovement(user));
		userDto.setFactionDto(dtoUtilService.dtoFromEntity(FactionDto.class, user.getFaction()));
		userDto.setHomePlanetDto(dtoUtilService.dtoFromEntity(PlanetDto.class, user.getHomePlanet()));
		userDto.setAlliance(dtoUtilService.dtoFromEntity(AllianceDto.class, user.getAlliance()));

		var galaxyData = user.getHomePlanet().getGalaxy();
		userDto.getHomePlanetDto().setGalaxyId(galaxyData.getId());
		userDto.getHomePlanetDto().setGalaxyName(galaxyData.getName());
		userDto.setConsumedEnergy(findConsumedEnergy(user));
		userDto.setMaxEnergy(findMaxEnergy(user));
		return userDto;
	}

	public boolean isBanned(Integer userId) {
		return userStorageRepository.isBanned(userId);
	}

	public List<UserStorage> findByLastMultiAccountCheckNewerThan(LocalDateTime date) {
		return userStorageRepository
				.findByLastMultiAccountCheckLessThanOrLastMultiAccountCheckIsNullOrderByLastMultiAccountCheckAsc(
						date,
						PageRequest.of(0, 50)
				);
	}

	/**
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.9.7
	 */
	public void emitUserData(UserStorage user) {
		socketIoService.sendMessage(user, "user_data_change", () -> findData(user));
	}

	private UserStorage convertTokenUserToUserStorage(TokenUser tokenUser) {
		var user = new UserStorage();
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
	 * @return the new value for the given resource
	 * @author Kevin Guanche Darias
	 */
	private Double calculateSum(Date now, Date lastAction, Double perSecondValue) {
		double difference = (now.getTime() - lastAction.getTime()) / (double) 1000;
		return (difference * perSecondValue);
	}

	/**
	 * Computes the resource per second that one faction resource has according to
	 * the current user improvement
	 *
	 * @param factionResource     The faction resource production
	 * @param resourceImprovement The improvement resource production
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private double computeUserResourcePerSecond(Float factionResource, Float resourceImprovement) {
		return improvementBo.computePlusPercertage(factionResource, resourceImprovement);
	}
}