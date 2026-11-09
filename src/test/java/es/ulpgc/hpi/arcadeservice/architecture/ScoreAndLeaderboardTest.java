package es.ulpgc.hpi.arcadeservice.architecture;

import es.ulpgc.hpi.arcadeservice.architecture.usecases.PlayerUseCaseImpl;
import es.ulpgc.hpi.arcadeservice.architecture.ports.PlayerRepositoryImpl;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScoreAndLeaderboardTest {
    private final PlayerUseCaseImpl players = new PlayerUseCaseImpl(new PlayerRepositoryImpl());

    @Test void accumulatesAndPersistsPoints() {
        var ada = players.create("Ada");
        assertEquals(10, players.addScore(ada.id(), 10).orElseThrow().score());
        var updated = players.addScore(ada.id(), 5).orElseThrow();
        assertEquals(15, updated.score());
        assertEquals("Ada", updated.nickname());
        assertEquals(updated, players.findById(ada.id()).orElseThrow());
    }

    @Test void rejectsNonPositivePointsWithoutChangingScore() {
        var ada = players.create("Ada");
        for (int points : new int[]{0, -1, -10}) {
            assertThrows(IllegalArgumentException.class, () -> players.addScore(ada.id(), points));
        }
        assertEquals(ada, players.findById(ada.id()).orElseThrow());
    }

    @Test void unknownPlayerDoesNotCreateARecord() {
        assertTrue(players.addScore(999, 10).isEmpty());
        assertTrue(players.list().isEmpty());
    }

    @Test void rejectsOverflowWithoutChangingScore() {
        var ada = players.create("Ada");
        players.addScore(ada.id(), Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> players.addScore(ada.id(), 1));
        assertEquals(Integer.MAX_VALUE, players.findById(ada.id()).orElseThrow().score());
    }

    @Test void sortsByScoreThenNicknameAndAppliesLimit() {
        var zoe = players.create("Zoe");
        var bob = players.create("Bob");
        var ada = players.create("Ada");
        players.addScore(zoe.id(), 10);
        players.addScore(ada.id(), 10);
        players.addScore(bob.id(), 20);
        assertEquals(List.of("Bob", "Ada", "Zoe"), players.leaderboard(null).stream().map(p -> p.nickname()).toList());
        assertEquals(List.of("Bob", "Ada"), players.leaderboard(2).stream().map(p -> p.nickname()).toList());
        assertEquals(3, players.leaderboard(99).size());
        assertEquals(1, players.leaderboard(1).size());
    }

    @Test void emptyLeaderboardIsAllowed() {
        assertTrue(players.leaderboard(null).isEmpty());
        assertTrue(players.leaderboard(1).isEmpty());
    }

    @Test void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> players.leaderboard(0));
        assertThrows(IllegalArgumentException.class, () -> players.leaderboard(-1));
    }

    @Test void zeroScoresAreIncludedInAlphabeticalOrder() {
        players.create("Zoe");
        players.create("Ada");
        assertEquals(List.of("Ada", "Zoe"), players.leaderboard(null).stream().map(p -> p.nickname()).toList());
    }
}
