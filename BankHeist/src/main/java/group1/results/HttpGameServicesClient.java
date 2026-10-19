package group1.results;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** HTTP transport only. Gameplay rules stay in the game. */
public final class HttpGameServicesClient {
    private final URI baseUri;
    private final HttpClient client;
    private final Gson gson = new Gson();

    public HttpGameServicesClient(URI baseUri) {
        if (!"http".equals(baseUri.getScheme()) && !"https".equals(baseUri.getScheme())) {
            throw new IllegalArgumentException("Game services URL must use HTTP or HTTPS.");
        }
        this.baseUri = URI.create(baseUri.toString().replaceAll("/+$", ""));
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public void register(PlayerProfile profile) throws IOException, InterruptedException {
        post("/players", profile);
    }

    public void submit(HeistResult result) throws IOException, InterruptedException {
        post("/heists", result);
    }

    private void post(String path, Object body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body))).build();
        check(client.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    public CompletableFuture<JsonObject> get(String path) {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(5)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            try {
                check(response);
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } catch (IOException failure) {
                throw new java.util.concurrent.CompletionException(failure);
            }
        });
    }

    private URI uri(String path) { return URI.create(baseUri + "/api/v1" + path); }

    private static void check(HttpResponse<String> response) throws ApiException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = "Game services returned HTTP " + response.statusCode();
            try {
                var body = JsonParser.parseString(response.body()).getAsJsonObject();
                if (body.has("message") && body.get("message").isJsonPrimitive()) {
                    String detail = body.get("message").getAsString();
                    message += ": " + detail.substring(0, Math.min(200, detail.length()));
                }
            } catch (RuntimeException ignored) { /* An error proxy may return HTML or an empty body. */ }
            throw new ApiException(response.statusCode(), message);
        }
    }

    public static final class ApiException extends IOException {
        private final int status;
        public ApiException(int status, String message) { super(message); this.status = status; }
        public int status() { return status; }
        public boolean retryable() { return status == 408 || status == 429 || status >= 500; }
    }
}
