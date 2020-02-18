/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.AllianceBo;
import com.kevinguanchedarias.owgejava.business.AllianceJoinRequestBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.AllianceJoinRequestDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.rest.trait.BaseRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@RequestMapping("game/alliance")
@ApplicationScope
public class AllianceRestService implements BaseRestServiceTrait {

	private static final String ALLIANCE_ID_KEY = "allianceId";
	private static final String JOIN_REQUEST_ID = "joinRequestId";

	@Autowired
	private AllianceBo allianceBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private AllianceJoinRequestBo allianceJoinRequestBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@Autowired
	private HttpServletRequest request;

	/**
	 * Finds all alliances
	 * 
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping(value = "")
	public List<AllianceDto> findAll() {
		return dtoUtilService.convertEntireArray(AllianceDto.class, allianceBo.findAll());
	}

	/**
	 * Finds Members for given alliances
	 * 
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping(value = "{id}/members")
	public List<UserStorageDto> members(@PathVariable("id") Integer id) {
		return dtoUtilService.convertEntireArray(UserStorageDto.class, allianceBo.findMembers(id)).stream()
				.map(current -> {
					current.setImprovements(null);
					current.setEmail(null);
					return current;
				}).collect(Collectors.toList());
	}

	/**
	 * Saves an alliance
	 * 
	 * @param allianceDto
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT })
	public AllianceDto save(@RequestBody AllianceDto allianceDto) {
		checkPost(allianceDto.getId(), request);
		Alliance alliance = dtoUtilService.entityFromDto(Alliance.class, allianceDto);
		alliance = allianceBo.save(alliance, userStorageBo.findLoggedIn().getId());
		return dtoUtilService.dtoFromEntity(AllianceDto.class, alliance);
	}

	@DeleteMapping()
	public void delete() {
		allianceBo.delete(userStorageBo.findLoggedIn());
	}

	@GetMapping(value = "/listRequest")
	public List<AllianceJoinRequestDto> listRequest() {
		UserStorage user = userStorageBo.findLoggedInWithDetails();
		Alliance alliance = user.getAlliance();
		if (user.getAlliance() == null) {
			throw new SgtBackendInvalidInputException("You don't have any alliance");
		} else if (!user.getAlliance().getOwner().getId().equals(user.getId())) {
			throw new SgtBackendInvalidInputException("You are not the owner of the alliance");
		}
		return dtoUtilService.convertEntireArray(AllianceJoinRequestDto.class,
				allianceJoinRequestBo.findByAlliance(alliance));
	}

	/**
	 * Returns my list of join request
	 * 
	 * @return
	 * @author Kevin Guanche Darias
	 * @since 0.8.1
	 */
	@GetMapping(value = "/my-requests")
	public List<AllianceJoinRequestDto> myRequests() {
		UserStorage user = userStorageBo.findLoggedIn();
		return dtoUtilService.convertEntireArray(AllianceJoinRequestDto.class,
				allianceJoinRequestBo.findByUserId(user.getId()));
	}

	@DeleteMapping("/my-requests/{id}")
	public void myRequestsDelete(@PathVariable Integer id) {
		allianceJoinRequestBo.delete(id);
	}

	@PostMapping(value = "/requestJoin")
	public AllianceJoinRequestDto join(@RequestBody Map<String, Integer> body) {
		checkMapEntry(body, ALLIANCE_ID_KEY);
		return allianceJoinRequestBo
				.toDto(allianceBo.requestJoin(body.get(ALLIANCE_ID_KEY), userStorageBo.findLoggedIn().getId()));
	}

	@PostMapping(value = "/acceptJoinRequest")
	public void acceptRequest(@RequestBody Map<String, Integer> body) {
		checkMapEntry(body, JOIN_REQUEST_ID);
		allianceBo.acceptJoin(body.get(JOIN_REQUEST_ID), userStorageBo.findLoggedIn().getId());
	}

	@PostMapping(value = "/rejectJoinRequest")
	public void rejectRequest(@RequestBody Map<String, Integer> body) {
		checkMapEntry(body, JOIN_REQUEST_ID);
		allianceBo.rejectJoin(body.get(JOIN_REQUEST_ID), userStorageBo.findLoggedIn().getId());
	}

	@PostMapping(value = "/leave")
	public void leave() {
		allianceBo.leave(userStorageBo.findLoggedIn().getId());
	}
}
