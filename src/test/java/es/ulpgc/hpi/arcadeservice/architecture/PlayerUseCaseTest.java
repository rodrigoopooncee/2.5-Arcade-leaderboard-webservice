package es.ulpgc.hpi.arcadeservice.architecture;

import es.ulpgc.hpi.arcadeservice.architecture.usecases.PlayerUseCaseImpl;
import es.ulpgc.hpi.arcadeservice.architecture.ports.PlayerRepositoryImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerUseCaseTest {
    @Test
    void createsAndFindsAPlayer() {
        var useCase = new PlayerUseCaseImpl(new PlayerRepositoryImpl());
        var player = useCase.create("Ada");
        assertEquals(1L, player.id());
        assertEquals(0, player.score());
        assertEquals(player, useCase.findById(player.id()).orElseThrow());
    }

    @Test
    void rejectsBlankNickname() {
        var useCase = new PlayerUseCaseImpl(new PlayerRepositoryImpl());
        assertThrows(IllegalArgumentException.class, () -> useCase.create(" "));
    }

    @Test
    void rejectsDuplicateNicknameAfterTrimming() {
        var useCase = new PlayerUseCaseImpl(new PlayerRepositoryImpl());
        var original = useCase.create("Ada");
        assertThrows(IllegalArgumentException.class, () -> useCase.create("  Ada  "));
        assertEquals(java.util.List.of(original), useCase.list());
        assertEquals(2L, useCase.create("Bob").id());
    }

    @Test
    void rejectsDuplicateNickname() {
        var useCase = new PlayerUseCaseImpl(new PlayerRepositoryImpl());
        useCase.create("Ada");
        assertThrows(IllegalArgumentException.class, () -> useCase.create("Ada"));
        assertEquals(1, useCase.list().size());
    }
}
