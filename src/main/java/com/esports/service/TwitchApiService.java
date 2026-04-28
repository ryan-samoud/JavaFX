package com.esports.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchApiService {

    public static class TwitchStream {
        private final String userName;
        private final String title;
        private final String language;
        private final int viewers;
        private final String twitchUrl;

        public TwitchStream(String userName, String title, String language, int viewers, String twitchUrl) {
            this.userName = userName;
            this.title = title;
            this.language = language;
            this.viewers = viewers;
            this.twitchUrl = twitchUrl;
        }

        public String getUserName() { return userName; }
        public String getTitle() { return title; }
        public String getLanguage() { return language; }
        public int getViewers() { return viewers; }
        public String getTwitchUrl() { return twitchUrl; }
    }

    private final String clientId = "xxxxxxxxxxxxxxxxxxxx";
    private final String clientSecret = "xxxxxxxxxxxxxxxxxxxxx";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();



    private String appAccessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public List<TwitchStream> getTopStreamsForGame(String gameName, int limit) {
        try {
            String token = getAppAccessToken();
            if (token == null || token.isBlank()) {
                return List.of();
            }
            String gameId = findGameId(gameName, token);
            if (gameId == null || gameId.isBlank()) {
                return List.of();
            }
            return fetchStreamsByGameId(gameId, token, Math.max(1, Math.min(limit, 10)));
        } catch (Exception e) {
            System.err.println("[TwitchApiService] " + e.getMessage());
            return List.of();
        }
    }

    private synchronized String getAppAccessToken() throws Exception {
        if (appAccessToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return appAccessToken;
        }

        // Guard against placeholder credentials.
        if (clientId.startsWith("abc123") || clientSecret.startsWith("secret123")) {
            throw new RuntimeException("Invalid Twitch credentials: replace placeholder client_id/client_secret with real Twitch app credentials.");
        }

        String tokenUrl = "https://id.twitch.tv/oauth2/token"
                + "?client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&grant_type=client_credentials";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Token request failed (" + response.statusCode() + "): " + response.body());
        }
        String bodyJson = response.body();
        appAccessToken = extractField(bodyJson, "access_token");
        if (appAccessToken == null || appAccessToken.isBlank()) {
            throw new RuntimeException("Token response missing access_token: " + bodyJson);
        }
        long expiresIn = parseLongOrDefault(extractField(bodyJson, "expires_in"), 3600L);
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        return appAccessToken;
    }

    private String findGameId(String gameName, String token) throws Exception {
        String url = "https://api.twitch.tv/helix/games?name=" + urlEncode(gameName);
        HttpRequest request = authedGet(url, token);
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        return extractFirstGameId(response.body());
    }

    private List<TwitchStream> fetchStreamsByGameId(String gameId, String token, int limit) throws Exception {
        String url = "https://api.twitch.tv/helix/streams?first=" + limit + "&game_id=" + urlEncode(gameId);
        HttpRequest request = authedGet(url, token);
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return List.of();
        }
        return parseStreams(response.body());
    }

    private HttpRequest authedGet(String url, String token) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String extractField(String json, String key) {
        if (json == null) return "";
        Pattern quotedPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher quotedMatcher = quotedPattern.matcher(json);
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1);
        }
        Pattern rawPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher rawMatcher = rawPattern.matcher(json);
        return rawMatcher.find() ? rawMatcher.group(1) : "";
    }

    private long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String extractFirstGameId(String json) {
        if (json == null || json.isBlank()) return null;
        Pattern p = Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private List<TwitchStream> parseStreams(String json) {
        if (json == null || json.isBlank()) return List.of();

        Pattern objectPattern = Pattern.compile("\\{[^\\{\\}]*\"user_name\"\\s*:\\s*\"[^\"]+\"[^\\{\\}]*\\}");
        Matcher objectMatcher = objectPattern.matcher(json);

        List<TwitchStream> streams = new ArrayList<>();
        while (objectMatcher.find()) {
            String obj = objectMatcher.group();
            String userName = defaultIfBlank(extractField(obj, "user_name"), "Unknown");
            String title = defaultIfBlank(extractField(obj, "title"), "Live stream");
            String language = defaultIfBlank(extractField(obj, "language"), "n/a");
            int viewers = (int) parseLongOrDefault(extractField(obj, "viewer_count"), 0L);
            streams.add(new TwitchStream(userName, title, language, viewers, "https://www.twitch.tv/" + userName));
        }
        return streams;
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
