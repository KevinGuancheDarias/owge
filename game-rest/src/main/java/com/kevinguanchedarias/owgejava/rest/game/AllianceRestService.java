package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.business.AllianceBo;
import com.kevinguanchedarias.owgejava.business.AllianceJoinRequestBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.AllianceJoinRequestDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import com.kevinguanchedarias.owgejava.repository.AllianceRepository;
import com.kevinguanchedarias.owgejava.rest.trait.BaseRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@RestController
@RequestMapping("game/alliance")
@ApplicationScope
@AllArgsConstructor
public class AllianceRestService implements BaseRestServiceTrait {

    private static final String ALLIANCE_ID_KEY = "allianceId";
    private static final String JOIN_REQUEST_ID = "joinRequestId";

    private final AllianceBo allianceBo;
    private final AllianceRepository allianceRepository;
    private final UserStorageBo userStorageBo;
    private final AllianceJoinRequestRepository allianceJoinRequestRepository;
    private final AllianceJoinRequestBo allianceJoinRequestBo;
    private final DtoUtilService dtoUtilService;
    private final HttpServletRequest request;

    /**
     * Finds all alliances
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @GetMapping(value = "")
    public List<AllianceDto> findAll() {
        return dtoUtilService.convertEntireArray(AllianceDto.class, allianceRepository.findAll());
    }

    /**
     * Finds Members for given alliances
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @GetMapping(value = "{id}/members")
    public List<UserStorageDto> members(@PathVariable("id") Integer id) {
        return dtoUtilService.convertEntireArray(UserStorageDto.class, allianceRepository.findMembers(id)).stream()
                .peek(current -> {
                    current.setImprovements(null);
                    current.setEmail(null);
                }).toList();
    }

    /**
     * Saves an alliance
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public AllianceDto save(@RequestBody AllianceDto allianceDto) {
        checkPost(allianceDto.getId(), request);
        var alliance = dtoUtilService.entityFromDto(Alliance.class, allianceDto);
        alliance = allianceBo.save(alliance, userStorageBo.findLoggedIn().getId());
        return dtoUtilService.dtoFromEntity(AllianceDto.class, alliance);
    }

    @DeleteMapping()
    public void delete() {
        allianceBo.delete(userStorageBo.findLoggedIn());
    }

    @GetMapping(value = "/listRequest")
    public List<AllianceJoinRequestDto> listRequest() {
        var user = userStorageBo.findLoggedInWithDetails();
        var alliance = user.getAlliance();
        if (user.getAlliance() == null) {
            throw new SgtBackendInvalidInputException("You don't have any alliance");
        } else if (!user.getAlliance().getOwner().getId().equals(user.getId())) {
            throw new SgtBackendInvalidInputException("You are not the owner of the alliance");
        }
        return dtoUtilService.convertEntireArray(AllianceJoinRequestDto.class,
                allianceJoinRequestRepository.findByAlliance(alliance));
    }

    /**
     * Returns my list of join request
     *
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    @GetMapping(value = "/my-requests")
    public List<AllianceJoinRequestDto> myRequests() {
        var user = userStorageBo.findLoggedIn();
        return dtoUtilService.convertEntireArray(AllianceJoinRequestDto.class,
                allianceJoinRequestRepository.findByUserId(user.getId()));
    }

    @DeleteMapping("/my-requests/{id}")
    public void myRequestsDelete(@PathVariable Integer id) {
        allianceJoinRequestRepository.deleteById(id);
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
