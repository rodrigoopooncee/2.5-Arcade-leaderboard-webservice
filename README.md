# Arcade Leaderboard Service

A small Java web service for players in an arcade competition. Each player has an ID, nickname, and total score. New players start with zero points. Data is stored in memory and resets when the service stops.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer

## Run

```bash
mvn test
mvn compile exec:java
```

The service listens on port `7070`. Set `PORT` to use another port.

If you work from the course's `2026:2027` folder on macOS, copy or clone this repository to a path without `:` before running Maven. Java uses `:` as a classpath separator.

## HTTP API

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/health` | Returns `{"status":"ok"}`. |
| `GET` | `/api/players` | Lists all players. |
| `GET` | `/api/players/{id}` | Returns one player, or `404` if it does not exist. |
| `POST` | `/api/players` | Creates a unique, case-sensitive, trimmed nickname from `{"nickname":"Ada"}`; returns `201` and a `Location` header. |

Example:

```bash
curl -i -X POST http://localhost:7070/api/players \
  -H 'Content-Type: application/json' \
  -d '{"nickname":"Ada"}'
```

## New operations (v1.1.0)

| Method | Path | Result |
| --- | --- | --- |
| `POST` | `/api/players/{id}/scores` | Body `{"points":10}`; adds to the total and returns the updated player (`200`). Positive points are required (`400` otherwise); unknown player with valid points returns `404`. |
| `GET` | `/api/leaderboard` | All players, descending score, then ascending nickname (Java String natural order). |
| `GET` | `/api/leaderboard?limit=2` | First two players after sorting. The optional limit must be a positive 32-bit integer; invalid values return `400`. |

A duplicate nickname (including surrounding spaces) returns `400`. `Ada` and `ada` remain distinct. Score overflow returns `400` without changing the stored score. Players with zero points appear in the ranking. A limit larger than the player count returns all available players.

### Windows PowerShell example

Start the service in one terminal, then use another:

```powershell
$base = "http://localhost:7070"
$ada = Invoke-RestMethod -Method Post -Uri "$base/api/players" -ContentType "application/json" -Body '{"nickname":"Ada"}'
Invoke-RestMethod -Method Post -Uri "$base/api/players/$($ada.id)/scores" -ContentType "application/json" -Body '{"points":10}'
Invoke-RestMethod -Method Post -Uri "$base/api/players/$($ada.id)/scores" -ContentType "application/json" -Body '{"points":5}'
Invoke-RestMethod "$base/api/leaderboard?limit=2"
```

Ada now has 15 points. Browser navigation can call GET endpoints; use PowerShell, curl or Postman for POST requests.

### curl example (Bash)

After registering the player, replace `1` with the returned ID:

```bash
curl -i -X POST http://localhost:7070/api/players/1/scores -H 'Content-Type: application/json' -d '{"points":10}'
curl -i 'http://localhost:7070/api/leaderboard?limit=2'
```

## Tests and local releases

Run `mvn clean test`. Tests cover duplicate nicknames, whitespace normalization, accumulation and persistence, invalid scores, unknown players, overflow, ranking and tie-breaking, limits, and HTTP integration through a real Javalin server on a free port.

All changes stay on `main`:

- `v1.0.0`: original service.
- `v1.0.1`: duplicate-nickname correction and regression test, without new features.
- `v1.1.0`: both features, tests and documentation; annotated tag.

```bash
git branch
git log --oneline --decorate
git show v1.0.1:src/main/java/es/ulpgc/hpi/arcadeservice/architecture/usecases/PlayerUseCase.java
git show v1.1.0 --no-patch
```

Keep the included `.git` directory to retain the history and local tags. Read `GUIA_ES.txt` for the Spanish walkthrough and `VALIDACION.txt` for the recorded validation results.

## Architecture

The project uses a small hexagonal architecture:

- `architecture/model/` contains `Player`, independently of HTTP and storage.
- `architecture/domain/` contains `PlayerFactory`, which applies the rules for creating a player.
- `architecture/ports/` contains `PlayerRepository` and its `PlayerRepositoryImpl` implementation.
- `architecture/usecases/` contains the `PlayerUseCase` interface and its `PlayerUseCaseImpl` implementation.
- `infrastructure/web/` contains the Javalin HTTP adapter and request data.
- `infrastructure/App.java` receives a use case and holds the Javalin application.
- `Main.java`, outside those packages, creates the repository and use case, then instantiates and starts `App`.

The use case depends on the interfaces in `architecture/ports/`, while Javalin stays in `infrastructure/`.
