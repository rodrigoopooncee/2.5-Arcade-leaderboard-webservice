package es.ulpgc.hpi.arcadeservice.architecture.usecases;

import es.ulpgc.hpi.arcadeservice.architecture.model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerUseCase {
    List<Player> list();

    Optional<Player> findById(long id);

    Player create(String nickname);

    Optional<Player> addScore(long id, int points);

    List<Player> leaderboard(Integer limit);
}
