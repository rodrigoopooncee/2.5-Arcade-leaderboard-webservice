package es.ulpgc.hpi.arcadeservice.infrastructure.web;

import es.ulpgc.hpi.arcadeservice.architecture.usecases.PlayerUseCase;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

import java.util.Map;
import java.util.Objects;

public final class PlayerHttpAdapter {
    private PlayerHttpAdapter() {
    }

    public static Javalin create(PlayerUseCase players) {
        Objects.requireNonNull(players);
        return Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.json(Map.of("status", "ok")));
            config.routes.get("/api/players", ctx -> ctx.json(players.list()));
            config.routes.get("/api/players/{id}", ctx -> {
                long id = Long.parseLong(ctx.pathParam("id"));
                ctx.json(players.findById(id).orElseThrow(() -> new NotFoundResponse("Player not found")));
            });
            config.routes.post("/api/players", ctx -> {
                var request = ctx.bodyAsClass(CreatePlayerRequest.class);
                var player = players.create(request.nickname());
                ctx.status(201);
                ctx.header("Location", "/api/players/" + player.id());
                ctx.json(player);
            });
            config.routes.post("/api/players/{id}/scores", ctx -> {
                long id = Long.parseLong(ctx.pathParam("id"));
                var request = ctx.bodyAsClass(RecordScoreRequest.class);
                ctx.json(players.addScore(id, request.points())
                        .orElseThrow(() -> new NotFoundResponse("Player not found")));
            });
            config.routes.get("/api/leaderboard", ctx -> {
                String rawLimit = ctx.queryParam("limit");
                Integer limit = rawLimit == null ? null : Integer.valueOf(rawLimit);
                ctx.json(players.leaderboard(limit));
            });
            config.routes.exception(com.fasterxml.jackson.core.JsonProcessingException.class, (error, ctx) ->
                ctx.status(400).json(Map.of("error", "Invalid JSON body")));
            config.routes.exception(IllegalArgumentException.class, (error, ctx) ->
                ctx.status(400).json(Map.of("error", error.getMessage())));
        });
    }
}
