package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.mock.ConfigurationMock;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import com.kevinguanchedarias.owgejava.repository.AllianceRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.*;
import static com.kevinguanchedarias.owgejava.mock.ConfigurationMock.givenConfigurationFalse;
import static com.kevinguanchedarias.owgejava.mock.ConfigurationMock.givenConfigurationTrue;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = AllianceBo.class
)
@MockBean({
        AllianceRepository.class,
        UserStorageRepository.class,
        ConfigurationBo.class,
        AllianceJoinRequestRepository.class,
        AuditBo.class,
})
class AllianceBoTest {
    private final AllianceBo allianceBo;
    private final AllianceRepository allianceRepository;
    private final UserStorageRepository userStorageRepository;
    private final AllianceJoinRequestRepository allianceJoinRequestRepository;
    private final ConfigurationBo configurationBo;
    private final AuditBo auditBo;

    @Autowired
    AllianceBoTest(
            AllianceBo allianceBo,
            AllianceRepository allianceRepository,
            UserStorageRepository userStorageRepository, AllianceJoinRequestRepository allianceJoinRequestRepository,
            ConfigurationBo configurationBo,
            AuditBo auditBo
    ) {
        this.allianceBo = allianceBo;
        this.allianceRepository = allianceRepository;
        this.userStorageRepository = userStorageRepository;
        this.allianceJoinRequestRepository = allianceJoinRequestRepository;
        this.configurationBo = configurationBo;
        this.auditBo = auditBo;
    }

    @Test
    void delete_should_work() {
        var alliance = givenAlliance();
        given(allianceRepository.findById(ALLIANCE_ID)).willReturn(Optional.of(alliance));

        allianceBo.delete(alliance);

        verify(userStorageRepository, times(1)).defineAllianceByAllianceId(alliance, null);
        verify(allianceRepository, times(1)).delete(alliance);
    }

    @Test
    void save_should_throw_when_alliances_are_disabled() {
        var alliance = givenAlliance();
        given(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE")).willReturn(givenConfigurationTrue());

        assertThatThrownBy(() -> allianceBo.save(alliance, USER_ID_1))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("while the idea is nice");
    }

    @Test
    void save_should_throw_when_creator_has_already_an_alliance() {
        var alliance = givenAlliance();
        alliance.setId(null);
        given(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE")).willReturn(givenConfigurationFalse());
        var user = givenUser1();
        user.setAlliance(givenAlliance(24));
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> allianceBo.save(alliance, USER_ID_1))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("already have an alliance");

    }

    @Test
    void save_should_throw_when_invoker_is_not_owner() {
        var alliance = givenAlliance();
        var user = givenUser1();
        alliance.setOwner(givenUser2());
        given(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE")).willReturn(givenConfigurationFalse());
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(allianceRepository.save(alliance)).willReturn(alliance);
        given(allianceRepository.findById(ALLIANCE_ID)).willReturn(Optional.of(alliance));

        testThrowsOwnerInvalid(() -> allianceBo.save(alliance, USER_ID_1));
    }

    @Test
    void save_should_properly_handle_new_save() {
        var alliance = givenAlliance();
        alliance.setId(null);
        given(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE")).willReturn(givenConfigurationFalse());
        var user = givenUser1();
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(allianceRepository.save(alliance)).willReturn(alliance);

        var retVal = allianceBo.save(alliance, USER_ID_1);

        var captor = ArgumentCaptor.forClass(Alliance.class);
        verify(allianceRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(retVal);
        assertThat(retVal.getOwner()).isEqualTo(user);
        assertThat(user.getAlliance()).isEqualTo(retVal);
        verify(userStorageRepository, times(1)).save(user);
    }

    @Test
    void save_should_properly_handle_update_save() {
        var alliance = givenAlliance();
        var user = givenUser1();
        alliance.setOwner(user);
        var allianceSpy = spy(alliance);
        given(configurationBo.findOrSetDefault("DISABLED_FEATURE_ALLIANCE", "FALSE")).willReturn(givenConfigurationFalse());
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(allianceRepository.save(alliance)).willReturn(allianceSpy);
        given(allianceRepository.findById(ALLIANCE_ID)).willReturn(Optional.of(allianceSpy));

        var retVal = allianceBo.save(alliance, USER_ID_1);

        verify(allianceSpy, times(1)).setName(ALLIANCE_NAME);
        verify(allianceSpy, times(1)).setDescription(ALLIANCE_DESCRIPTION);
        assertThat(retVal).isSameAs(allianceSpy);
    }

    @Test
    void areEnemies_should_be_true_when_source_has_no_alliance() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        targetUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isTrue();
    }

    @Test
    void areEnemies_should_be_true_when_target_has_no_alliance() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        sourceUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isTrue();
    }

    @Test
    void areEnemies_should_be_true_when_both_has_different_non_null_alliances() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        sourceUser.setAlliance(givenAlliance(12));
        targetUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isTrue();
    }

    @Test
    void areEnemies_should_be_false_when_both_users_has_same_alliance() {
        var sourceUser = givenUser1();
        var targetUser = givenUser2();
        sourceUser.setAlliance(givenAlliance());
        targetUser.setAlliance(givenAlliance());

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isFalse();
    }

    @Test
    void areEnemies_should_handle_userIds() {
        var sourceUser = givenUser1();
        var targetUser = givenUser1();

        assertThat(allianceBo.areEnemies(sourceUser, targetUser)).isFalse();
    }

    @Test
    void requestJoin_should_work() {
        var alliance = givenAlliance();
        var user = givenUser1();
        given(allianceRepository.findById(ALLIANCE_ID)).willReturn(Optional.of(alliance));
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(allianceJoinRequestRepository.save(any(AllianceJoinRequest.class))).will(returnsFirstArg());

        var retVal = allianceBo.requestJoin(ALLIANCE_ID, USER_ID_1);

        var captor = ArgumentCaptor.forClass(AllianceJoinRequest.class);
        verify(allianceJoinRequestRepository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved).isSameAs(retVal);
        assertThat(saved.getAlliance()).isEqualTo(alliance);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getRequestDate()).isNotNull();
    }

    @Test
    void requestJoin_should_throw_when_invoker_already_has_alliance() {
        var alliance = givenAlliance();
        var user = givenUser1();
        user.setAlliance(givenAlliance(24));
        given(allianceRepository.findById(ALLIANCE_ID)).willReturn(Optional.of(alliance));
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(allianceJoinRequestRepository.save(any(AllianceJoinRequest.class))).will(returnsFirstArg());

        assertThatThrownBy(() -> allianceBo.requestJoin(ALLIANCE_ID, USER_ID_1))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("nice try");
    }

    @Test
    void acceptJoin_should_throw_when_not_owner() {
        var request = givenAllianceJoinRequest();
        var alliance = request.getAlliance();
        alliance.setOwner(givenUser2());
        given(allianceJoinRequestRepository.findById(ALLIANCE_JOIN_REQUEST_ID)).willReturn(Optional.of(request));

        testThrowsOwnerInvalid(() -> allianceBo.acceptJoin(ALLIANCE_JOIN_REQUEST_ID, USER_ID_1));
    }

    @ParameterizedTest
    @CsvSource({
            "100,7,14",
            "100,0,5",
            "1000,7,16"
    })
    void acceptJoin_should_throw_when_limit_reached(long userCount, String percentage, int countByAlliance) {
        var request = givenAllianceJoinRequest();
        var alliance = request.getAlliance();
        var user = request.getUser();
        alliance.setOwner(user);
        given(allianceJoinRequestRepository.findById(ALLIANCE_JOIN_REQUEST_ID)).willReturn(Optional.of(request));
        given(userStorageRepository.count()).willReturn(userCount);
        given(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE_PERCENTAGE", "7")).willReturn(ConfigurationMock.givenConfiguration(percentage));
        given(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE", "15")).willReturn(ConfigurationMock.givenConfiguration("15"));
        given(userStorageRepository.countByAlliance(alliance)).willReturn(countByAlliance);

        assertThatThrownBy(() -> allianceBo.acceptJoin(ALLIANCE_JOIN_REQUEST_ID, USER_ID_1))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage("I18N_ERR_ALLIANCE_IS_FULL");
    }

    @Test
    void acceptJoin_should_work() {
        var request = givenAllianceJoinRequest();
        var alliance = request.getAlliance();
        var user = givenUser1();
        int requesterId = 190;
        var requester = givenUser(requesterId);
        alliance.setUsers(List.of(user));
        request.setUser(requester);
        alliance.setOwner(user);
        given(allianceJoinRequestRepository.findById(ALLIANCE_JOIN_REQUEST_ID)).willReturn(Optional.of(request));
        given(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE_PERCENTAGE", "7")).willReturn(ConfigurationMock.givenConfiguration("7"));
        given(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE", "15")).willReturn(ConfigurationMock.givenConfiguration("15"));
        given(userStorageRepository.count()).willReturn(1000L);

        allianceBo.acceptJoin(ALLIANCE_JOIN_REQUEST_ID, USER_ID_1);

        assertThat(requester.getAlliance()).isEqualTo(alliance);
        verify(auditBo, times(1)).nonRequestAudit(AuditActionEnum.USER_INTERACTION, "JOIN_ALLIANCE", requester, USER_ID_1);
        verify(auditBo, times(1)).doAudit(AuditActionEnum.ACCEPT_JOIN_ALLIANCE, null, requesterId);
        verify(userStorageRepository, times(1)).save(requester);
        verify(allianceJoinRequestRepository, times(1)).deleteByUser(requester);
    }

    @Test
    void acceptJoin_should_ignore_and_just_delete_the_request_when_requester_already_has_an_alliance() {
        var request = givenAllianceJoinRequest();
        var alliance = request.getAlliance();
        var user = givenUser1();
        int requesterId = 190;
        var requester = givenUser(requesterId);
        requester.setAlliance(givenAlliance(232));
        alliance.setUsers(List.of(user));
        request.setUser(requester);
        alliance.setOwner(user);
        given(allianceJoinRequestRepository.findById(ALLIANCE_JOIN_REQUEST_ID)).willReturn(Optional.of(request));
        given(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE_PERCENTAGE", "7")).willReturn(ConfigurationMock.givenConfiguration("7"));
        given(configurationBo.findOrSetDefault("ALLIANCE_MAX_SIZE", "15")).willReturn(ConfigurationMock.givenConfiguration("15"));
        given(userStorageRepository.count()).willReturn(1000L);

        allianceBo.acceptJoin(ALLIANCE_JOIN_REQUEST_ID, USER_ID_1);

        verifyNoInteractions(auditBo);
        verify(userStorageRepository, never()).save(any(UserStorage.class));
        verify(allianceJoinRequestRepository, times(1)).delete(request);
    }

    @Test
    void rejectJoin_should_throw_when_invoker_is_not_owner() {
        var request = givenAllianceJoinRequest();
        var alliance = request.getAlliance();
        alliance.setOwner(givenUser2());
        given(allianceJoinRequestRepository.findById(ALLIANCE_JOIN_REQUEST_ID)).willReturn(Optional.of(request));

        testThrowsOwnerInvalid(() -> allianceBo.rejectJoin(ALLIANCE_JOIN_REQUEST_ID, USER_ID_1));

        verify(allianceJoinRequestRepository, never()).delete(any(AllianceJoinRequest.class));
    }

    @Test
    void rejectJoin_should_work() {
        var request = givenAllianceJoinRequest();
        var alliance = request.getAlliance();
        alliance.setOwner(givenUser1());
        given(allianceJoinRequestRepository.findById(ALLIANCE_JOIN_REQUEST_ID)).willReturn(Optional.of(request));

        allianceBo.rejectJoin(ALLIANCE_JOIN_REQUEST_ID, USER_ID_1);

        verify(allianceJoinRequestRepository, times(1)).delete(request);
    }

    @Test
    void leave_should_work() {
        var user = givenUser1();
        var alliance = givenAlliance();
        user.setAlliance(alliance);
        given(userStorageRepository.getReferenceById(USER_ID_1)).willReturn(user);

        allianceBo.leave(USER_ID_1);

        assertThat(user.getAlliance()).isNull();
        verify(userStorageRepository, times(1)).save(user);
    }

    @Test
    void leave_should_throw_when_attempting_to_leave_owned_alliance() {
        var user = givenUser1();
        var alliance = givenAlliance();
        user.setAlliance(alliance);
        given(userStorageRepository.getReferenceById(USER_ID_1)).willReturn(user);
        given(allianceRepository.findOneByOwnerId(USER_ID_1)).willReturn(alliance);

        assertThatThrownBy(() -> allianceBo.leave(USER_ID_1))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("your own alliance");
        verify(userStorageRepository, never()).save(any());
    }

    @Test
    void deleteByUser_should_throw_on_null_alliance() {
        var user = givenUser1();
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> allianceBo.deleteByUser(user))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("have any alliance");
        verify(allianceRepository, never()).delete(any());
    }

    @Test
    void deleteByUser_should_throw_on_non_owned_alliance() {
        var user = givenUser1();
        var alliance = givenAlliance();
        alliance.setOwner(givenUser2());
        user.setAlliance(alliance);
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));

        testThrowsOwnerInvalid(() -> allianceBo.deleteByUser(user));
        verify(allianceRepository, never()).delete(any());
    }

    @Test
    void deleteByUser_should_work() {
        var user = givenUser1();
        var alliance = givenAlliance();
        alliance.setOwner(user);
        user.setAlliance(alliance);
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(allianceRepository.findById(ALLIANCE_ID)).willReturn(Optional.of(alliance));

        allianceBo.deleteByUser(user);

        verify(userStorageRepository, times(1)).defineAllianceByAllianceId(alliance, null);
        verify(allianceRepository, times(1)).delete(alliance);
    }

    private void testThrowsOwnerInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("try hacking the owner account");
    }
}
