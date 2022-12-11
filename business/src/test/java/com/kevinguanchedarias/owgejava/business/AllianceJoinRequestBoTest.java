package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.mock.AllianceMock;
import com.kevinguanchedarias.owgejava.repository.AllianceJoinRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = AllianceJoinRequestBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(AllianceJoinRequestRepository.class)
class AllianceJoinRequestBoTest {
    private final AllianceJoinRequestBo allianceJoinRequestBo;
    private final AllianceJoinRequestRepository repository;

    @Autowired
    AllianceJoinRequestBoTest(AllianceJoinRequestBo allianceJoinRequestBo, AllianceJoinRequestRepository repository) {
        this.allianceJoinRequestBo = allianceJoinRequestBo;
        this.repository = repository;
    }

    @Test
    void save_should_throw_on_modification_attempt() {
        var request = AllianceMock.givenAllianceJoinRequest();

        assertThatThrownBy(() -> allianceJoinRequestBo.save(request))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("modify a join request");

        verifyNoInteractions(repository);
    }

    @Test
    void save_should_throw_when_trying_to_request_join_twice() {
        var request = AllianceMock.givenAllianceJoinRequest(null);
        given(repository.existsByUserAndAlliance(request.getUser(), request.getAlliance())).willReturn(true);

        assertThatThrownBy(() -> allianceJoinRequestBo.save(request))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("already have a join request");
        verify(repository, never()).save(any(AllianceJoinRequest.class));
    }

    @Test
    void save_should_work() {
        var request = AllianceMock.givenAllianceJoinRequest(null);
        request.setRequestDate(null);
        given(repository.save(request)).will(returnsFirstArg());

        var retVal = allianceJoinRequestBo.save(request);

        assertThat(retVal.getRequestDate()).isNotNull();
        assertThat(retVal).isSameAs(request);
    }
}
