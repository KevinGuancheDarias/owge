package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.RequirementGroupBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.PostLoad;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.RequirementGroupMock.givenRequirementGroup;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = EntityWithRequirementGroupsListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(RequirementGroupBo.class)
class EntityWithRequirementGroupsListenerTest {
    private final EntityWithRequirementGroupsListener entityWithRequirementGroupsListener;
    private final RequirementGroupBo requirementGroupBo;

    @Autowired
    public EntityWithRequirementGroupsListenerTest(
            EntityWithRequirementGroupsListener entityWithRequirementGroupsListener,
            RequirementGroupBo requirementGroupBo
    ) {
        this.entityWithRequirementGroupsListener = entityWithRequirementGroupsListener;
        this.requirementGroupBo = requirementGroupBo;
    }

    @SneakyThrows
    @Test
    void loadRequirements_should_work() {
        var requirementGroups = List.of(givenRequirementGroup());
        var entityWithRequirementGroupsMock = mock(EntityWithRequirementGroups.class);
        given(requirementGroupBo.findRequirements(entityWithRequirementGroupsMock)).willReturn(requirementGroups);

        entityWithRequirementGroupsListener.loadRequirements(entityWithRequirementGroupsMock);

        Assertions.assertThat(
                EntityWithRequirementGroupsListener.class.getMethod("loadRequirements", EntityWithRequirementGroups.class).getAnnotation(PostLoad.class)
        ).isNotNull();
        verify(entityWithRequirementGroupsMock, times(1)).setRequirementGroups(requirementGroups);
    }
}
