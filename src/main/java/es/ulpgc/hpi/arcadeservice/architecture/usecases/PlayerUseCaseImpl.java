package es.ulpgc.hpi.arcadeservice.architecture.usecases;

import es.ulpgc.hpi.arcadeservice.architecture.domain.PlayerFactory;
import es.ulpgc.hpi.arcadeservice.architecture.ports.PlayerRepository;
import es.ulpgc.hpi.arcadeservice.architecture.model.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class PlayerUseCaseImpl implements PlayerUseCase {
    private final PlayerRepository repository;
    private final AtomicLong nextId = new AtomicLong(1);

    public PlayerUseCaseImpl(PlayerRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public List<Player> list() {
        return repository.findAll();
    }

    @Override
    public Optional<Player> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public synchronized Player create(String nickname) {
        if (nickname != null && repository.findAll().stream()
                .anyMatch(player -> player.nickname().equals(nickname.trim()))) {
            throw new IllegalArgumentException("Nickname already exists");
        }
        var player = PlayerFactory.create(nickname, nextId::getAndIncrement);
        repository.save(player);
        return player;
    }
}
