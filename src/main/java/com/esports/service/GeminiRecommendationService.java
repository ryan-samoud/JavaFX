package com.esports.service;

import com.esports.model.Jeu;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiRecommendationService {
    private static final String MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_API_KEY = "wwwwwwwwwwww-wwwwww";
    private static final int MAX_RETRIES_ON_503 = 3;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String recommend(String userPrompt, List<Jeu> jeux) throws IOException, InterruptedException {
        String apiKey = readApiKey();
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent?key=" + apiKey;

        String catalog = buildCatalogContext(jeux);
        String prompt = buildPrompt(userPrompt, catalog);
        String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + jsonEscape(prompt) + "\"}]}]}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= MAX_RETRIES_ON_503; attempt++) {
            response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 503 && attempt < MAX_RETRIES_ON_503) {
                try {
                    Thread.sleep(700L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                continue;
            }
            break;
        }

        if (response == null) {
            return "Gemini request failed: empty response.";
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "Gemini request failed (" + response.statusCode() + "): " + response.body();
        }
        return extractResponseText(response.body());
    }

    private String readApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        return (env == null || env.isBlank()) ? DEFAULT_API_KEY : env.trim();
    }

    private String buildCatalogContext(List<Jeu> jeux) {
        if (jeux == null || jeux.isEmpty()) {
            return "No games available in DB.";
        }
        return jeux.stream()
                .limit(80)
                .map(j -> "- " + safe(j.getNom(), "Sans nom")
                        + " | categorie=" + (j.getCategorie() != null ? safe(j.getCategorie().getNomCategorie(), "N/A") : "N/A")
                        + " | mode=" + safe(j.getMode(), "N/A")
                        + " | age=" + j.getTrancheAge()
                        + " | players=" + j.getNbJoueurs()
                        + " | note=" + j.getNote()
                        + " | description=" + safe(j.getDescription(), "N/A"))
                .collect(Collectors.joining("\n"));
    }

    private String buildPrompt(String userPrompt, String catalog) {
        return "You are a game recommendation assistant for a JavaFX app.\n"
                + "Use ONLY games from the provided DB catalog. If user asks outside catalog, explain politely.\n"
                + "Return concise recommendation with: top picks, why each fits, and one backup.\n"
                + "IMPORTANT: mention exact game names as they appear in catalog. Start with a section named 'Recommended Games:'.\n\n"
                + "DB catalog:\n" + catalog + "\n\n"
                + "User request:\n" + safe(userPrompt, "Recommend me a good game");
    }

    private String extractResponseText(String json) {
        if (json == null || json.isBlank()) {
            return "No response from Gemini.";
        }
        String key = "\"text\":";
        int idx = json.indexOf(key);
        if (idx < 0) {
            return "No text field in Gemini response.\nRaw: " + json;
        }
        int start = json.indexOf('"', idx + key.length());
        if (start < 0) {
            return "Could not parse Gemini response.";
        }
        int i = start + 1;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? "Gemini returned empty text." : text;
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
