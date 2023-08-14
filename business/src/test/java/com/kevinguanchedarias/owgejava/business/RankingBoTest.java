package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.pojo.RankingEntry;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.*;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = RankingBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UserStorageRepository.class)
class RankingBoTest {
    private static final double USER_1_POINTS = 4400;
    private static final double USER_2_POINTS = 8800;
    private final RankingBo rankingBo;
    private final UserStorageRepository userStorageRepository;

    @Autowired
    RankingBoTest(RankingBo rankingBo, UserStorageRepository userStorageRepository) {
        this.rankingBo = rankingBo;
        this.userStorageRepository = userStorageRepository;
    }

    @Test
    void findRanking_should_work() {
        var user1 = givenUser1();
        var userFaction = givenFaction();
        user1.setPoints(USER_1_POINTS);
        user1.setAlliance(givenAlliance());
        user1.setFaction(userFaction);
        var user2 = givenUser2();
        user2.setPoints(USER_2_POINTS);
        user2.setFaction(userFaction);
        given(userStorageRepository.findAllByOrderByPointsDesc()).willReturn(List.of(user2, user1));

        var retVal = rankingBo.findRanking();

        assertThat(retVal).hasSize(2);
        var firstPosition = retVal.get(0);
        var secondPosition = retVal.get(1);
        testPosition(firstPosition, 1, USER_2_POINTS, USER_ID_2, USER_2_NAME, null, null);
        assertThat(firstPosition.faction().getName()).isEqualTo(FACTION_NAME);
        assertThat(firstPosition.faction().getDescription()).isEqualTo(FACTION_DESCRIPTION);
        assertThat(firstPosition.faction().getImageUrl()).isNotEmpty();
        testPosition(secondPosition, 2, USER_1_POINTS, USER_ID_1, USER_1_NAME, ALLIANCE_ID, ALLIANCE_NAME);
    }

    private void testPosition(
            RankingEntry entry,
            int expectedPosition,
            double expectedPoints,
            int expectedId,
            String expectedUsername,
            Integer expectedAllianceId,
            String expectedAllianceName
    ) {
        assertThat(entry.position()).isEqualTo(expectedPosition);
        assertThat(entry.points()).isEqualTo(expectedPoints);
        assertThat(entry.userId()).isEqualTo(expectedId);
        assertThat(entry.username()).isEqualTo(expectedUsername);
        assertThat(entry.allianceId()).isEqualTo(expectedAllianceId);
        assertThat(entry.allianceName()).isEqualTo(expectedAllianceName);
    }
}
