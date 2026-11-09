package es.ulpgc.hpi.arcadeservice.infrastructure.web;

import es.ulpgc.hpi.arcadeservice.architecture.usecases.PlayerUseCaseImpl;
import es.ulpgc.hpi.arcadeservice.architecture.ports.PlayerRepositoryImpl;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerHttpAdapterTest {
    private Javalin server;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void start() {
        server = PlayerHttpAdapter.create(new PlayerUseCaseImpl(new PlayerRepositoryImpl())).start(0);
        base = "http://localhost:" + server.port();
    }
    @AfterEach void stop() { if (server != null) server.stop(); }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
    private long create(String nickname) throws Exception {
        var response = request("POST", "/api/players", "{\"nickname\":\"" + nickname + "\"}");
        assertEquals(201, response.statusCode());
        long id = json.readTree(response.body()).get("id").asLong();
        assertEquals("/api/players/" + id, response.headers().firstValue("Location").orElseThrow());
        return id;
    }

    @Test void originalEndpointsAndDuplicateRegression() throws Exception {
        assertEquals(200, request("GET", "/health", null).statusCode());
        long id = create("Ada");
        assertEquals(200, request("GET", "/api/players/" + id, null).statusCode());
        assertEquals(404, request("GET", "/api/players/999", null).statusCode());
        assertEquals(400, request("POST", "/api/players", "{\"nickname\":\"  Ada  \"}").statusCode());
        assertEquals(1, json.readTree(request("GET", "/api/players", null).body()).size());
    }

    @Test void scoresAndLeaderboardWorkTogether() throws Exception {
        long zoe = create("Zoe"), ada = create("Ada"), bob = create("Bob");
        for (long id : new long[]{zoe, ada, bob}) {
            assertEquals(200, request("POST", "/api/players/" + id + "/scores", "{\"points\":10}").statusCode());
        }
        var updated = request("POST", "/api/players/" + bob + "/scores", "{\"points\":5}");
        assertEquals(200, updated.statusCode());
        assertEquals(15, json.readTree(updated.body()).get("score").asInt());
        assertEquals(15, json.readTree(request("GET", "/api/players/" + bob, null).body()).get("score").asInt());
        var response = request("GET", "/api/leaderboard", null);
        assertEquals(200, response.statusCode());
        var ranking = json.readTree(response.body());
        assertEquals(3, ranking.size());
        assertEquals("Bob", ranking.get(0).get("nickname").asText());
        assertEquals("Ada", ranking.get(1).get("nickname").asText());
        assertEquals("Zoe", ranking.get(2).get("nickname").asText());
        var limited = request("GET", "/api/leaderboard?limit=2", null);
        assertEquals(200, limited.statusCode());
        assertEquals(2, json.readTree(limited.body()).size());
        assertEquals(3, json.readTree(request("GET", "/api/leaderboard?limit=99", null).body()).size());
    }

    @Test void invalidScoresAndUnknownPlayer() throws Exception {
        long id = create("Ada");
        for (String body : new String[]{"{\"points\":0}", "{\"points\":-5}", "{}", "invalid-json"}) {
            assertEquals(400, request("POST", "/api/players/" + id + "/scores", body).statusCode());
        }
        assertEquals(404, request("POST", "/api/players/999/scores", "{\"points\":10}").statusCode());
        assertEquals(0, json.readTree(request("GET", "/api/players/" + id, null).body()).get("score").asInt());
    }

    @Test void invalidLimitsAndEmptyLeaderboard() throws Exception {
        var empty = request("GET", "/api/leaderboard", null);
        assertEquals(200, empty.statusCode());
        assertEquals(0, json.readTree(empty.body()).size());
        for (String limit : new String[]{"0", "-1", "abc", "1.5", "", "2147483648"}) {
            assertEquals(400, request("GET", "/api/leaderboard?limit=" + limit, null).statusCode(), "limit=" + limit);
        }
    }
}
