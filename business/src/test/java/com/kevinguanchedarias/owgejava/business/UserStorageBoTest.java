package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.SgtFactionNotFoundException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;

import static com.kevinguanchedarias.owgejava.mock.FactionMock.*;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.TokenUserMock.TOKEN_USER_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UserStorageBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageRepository.class,
        FactionBo.class,
        PlanetRepository.class,
        PlanetBo.class,
        RequirementBo.class,
        AllianceBo.class,
        ImprovementBo.class,
        EntityManager.class,
        FactionSpawnLocationBo.class,
        AuditBo.class,
        UserEventEmitterBo.class,
        TransactionUtilService.class,
        UserSessionService.class
})
class UserStorageBoTest {
    private final UserStorageBo userStorageBo;
    private final UserStorageRepository repository;

    private final FactionBo factionBo;
    private final PlanetBo planetBo;
    private final PlanetRepository planetRepository;
    private final RequirementBo requirementBo;
    private final AuditBo auditBo;
    private final FactionSpawnLocationBo factionSpawnLocationBo;
    private final TransactionUtilService transactionUtilService;
    private final EntityManager entityManager;
    private final UserEventEmitterBo userEventEmitterBo;
    private final UserSessionService userSessionService;

    @Autowired
    UserStorageBoTest(
            UserStorageBo userStorageBo,
            UserStorageRepository repository,
            FactionBo factionBo,
            PlanetBo planetBo,
            PlanetRepository planetRepository,
            RequirementBo requirementBo,
            AuditBo auditBo,
            FactionSpawnLocationBo factionSpawnLocationBo,
            TransactionUtilService transactionUtilService,
            EntityManager entityManager,
            UserEventEmitterBo userEventEmitterBo,
            UserSessionService userSessionService
    ) {
        this.userStorageBo = userStorageBo;
        this.repository = repository;
        this.factionBo = factionBo;
        this.planetBo = planetBo;
        this.planetRepository = planetRepository;
        this.requirementBo = requirementBo;
        this.auditBo = auditBo;
        this.factionSpawnLocationBo = factionSpawnLocationBo;
        this.transactionUtilService = transactionUtilService;
        this.entityManager = entityManager;
        this.userEventEmitterBo = userEventEmitterBo;
        this.userSessionService = userSessionService;
    }

    @Test
    void subscribe_should_throw_if_faction_does_not_exists() {
        assertThatThrownBy(() -> userStorageBo.subscribe(FACTION_ID))
                .isInstanceOf(SgtFactionNotFoundException.class);
    }

    @Test
    void subscribe_should_do_nothing_if_user_already_exists_in_the_universe() {
        given(factionBo.exists(FACTION_ID)).willReturn(true);
        given(repository.existsById(TOKEN_USER_ID)).willReturn(true);
        given(userSessionService.findLoggedIn()).willReturn(givenUser1());

        assertThat(userStorageBo.subscribe(FACTION_ID)).isFalse();
        verifyNoInteractions(planetBo, planetRepository, requirementBo, auditBo);
    }

    @ParameterizedTest
    @CsvSource({
            "0,0,false",
            "1,1,true"
    })
    void subscribe_should_work(Integer userId, int timesAuditSubscribe, boolean subscribeIsSuccess) {
        var faction = givenFaction();
        var homePlanet = givenSourcePlanet();
        given(userSessionService.findLoggedIn()).willReturn(givenUser(userId));
        given(factionBo.exists(FACTION_ID)).willReturn(true);
        given(factionBo.findById(FACTION_ID)).willReturn(faction);
        given(factionSpawnLocationBo.determineSpawnGalaxy(faction)).willReturn(GALAXY_ID);
        given(planetBo.findRandomPlanet(GALAXY_ID)).willReturn(homePlanet);
        given(repository.save(any(UserStorage.class))).will(returnsFirstArg());

        var retVal = userStorageBo.subscribe(FACTION_ID);

        var userSaveCaptor = ArgumentCaptor.forClass(UserStorage.class);
        verify(repository, times(1)).save(userSaveCaptor.capture());
        var savedUser = userSaveCaptor.getValue();
        assertThat(savedUser.getFaction()).isEqualTo(faction);
        assertThat(savedUser.getHomePlanet()).isEqualTo(homePlanet);
        assertThat(savedUser.getPrimaryResource()).isEqualTo(FACTION_INITIAL_PR);
        assertThat(savedUser.getSecondaryResource()).isEqualTo(FACTION_INITIAL_SR);
        assertThat(savedUser.getEnergy()).isEqualTo(FACTION_INITIAL_ENERGY);
        assertThat(savedUser.getLastAction()).isNotNull();
        assertThat(homePlanet.getOwner().getId()).isEqualTo(userId);
        assertThat(homePlanet.getHome()).isTrue();
        verify(planetRepository, times(1)).save(homePlanet);
        verify(requirementBo, times(1)).triggerFactionSelection(savedUser);
        verify(requirementBo, times(1)).triggerHomeGalaxySelection(savedUser);
        verify(auditBo, times(timesAuditSubscribe)).doAudit(AuditActionEnum.SUBSCRIBE_TO_WORLD);
        assertThat(retVal).isEqualTo(subscribeIsSuccess);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,0",
            "5,1"
    }, nullValues = "null")
    void save_should_work(Integer userId, int timesRefreshAndEmit) {
        var user = givenUser(userId);
        given(repository.save(user)).willReturn(user);
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        var retVal = userStorageBo.save(user);

        assertThat(retVal).isSameAs(user);
        verify(entityManager, times(timesRefreshAndEmit)).refresh(user);
        verify(userEventEmitterBo, times(timesRefreshAndEmit)).emitUserData(user);
    }

    @Test
    void save_should_save_but_not_emit() {
        var user = givenUser1();
        given(repository.save(user)).willReturn(user);

        var retVal = userStorageBo.save(user, false);

        assertThat(retVal).isSameAs(user);
        verifyNoInteractions(transactionUtilService, entityManager, userEventEmitterBo);
    }
}
