package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRelation;
import com.kevinguanchedarias.owgejava.entity.EntityWithRelationImp;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = EntityWithRelationListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(ObjectRelationBo.class)
class EntityWithRelationListenerTest {
    private final EntityWithRelationListener entityWithRelationListener;
    private final ObjectRelationBo objectRelationBo;

    @Autowired
    public EntityWithRelationListenerTest(EntityWithRelationListener entityWithRelationListener, ObjectRelationBo objectRelationBo) {
        this.entityWithRelationListener = entityWithRelationListener;
        this.objectRelationBo = objectRelationBo;
    }

    @SneakyThrows
    @Test
    void defineRelation_should_work() {
        var entityWithRelation = mock(EntityWithRelation.class);
        var objectType = ObjectEnum.UNIT;
        var or = givenObjectRelation();

        given(entityWithRelation.getObject()).willReturn(objectType);
        given(entityWithRelation.getId()).willReturn(UNIT_ID_1);
        given(objectRelationBo.findOne(objectType, UNIT_ID_1)).willReturn(or);

        entityWithRelationListener.defineRelation(entityWithRelation);

        verify(entityWithRelation, times(1)).setRelation(or);
        assertThat(
                EntityWithRelationListener.class.getMethod("defineRelation", EntityWithRelation.class).getAnnotation(PostLoad.class)
        ).isNotNull();
    }

    @SneakyThrows
    @Test
    void saveRelation_should_work() {
        var entityWithRelation = mock(EntityWithRelation.class);
        var objectType = ObjectEnum.UNIT;
        var or = givenObjectRelation();

        given(entityWithRelation.getObject()).willReturn(objectType);
        given(entityWithRelation.getId()).willReturn(UNIT_ID_1);
        given(objectRelationBo.create(objectType, UNIT_ID_1)).willReturn(or);

        entityWithRelationListener.saveRelation(entityWithRelation);

        verify(entityWithRelation, times(1)).setRelation(or);
        assertThat(
                EntityWithRelationListener.class.getMethod("saveRelation", EntityWithRelation.class).getAnnotation(PostPersist.class)
        ).isNotNull();
    }

    @SneakyThrows
    @Test
    void removeRelation_should_work() {
        var entityWithRelation = mock(EntityWithRelationImp.class);
        var or = givenObjectRelation();
        given(entityWithRelation.getRelation()).willReturn(or);

        entityWithRelationListener.removeRelation(entityWithRelation);

        verify(objectRelationBo, times(1)).delete(or);
        assertThat(
                EntityWithRelationListener.class.getMethod("removeRelation", EntityWithRelationImp.class).getAnnotation(PreRemove.class)
        ).isNotNull();
    }

}
