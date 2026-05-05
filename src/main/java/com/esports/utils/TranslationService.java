package com.esports.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * UTILS — TranslationService.java
 * Uses MyMemory free API (no key needed, 1000 words/day free).
 * Translates text from French to English.
 */
public class TranslationService {

    private static final String API_URL =
            "https://api.mymemory.translated.net/get?q=%s&langpair=fr|en";

    /**
     * Translate French text to English.
     * Returns original text if translation fails.
     */
    public static String translateToEnglish(String frenchText) {
        if (frenchText == null || frenchText.isBlank()) return frenchText;

        try {
            String encoded = URLEncoder.encode(frenchText, StandardCharsets.UTF_8);
            String urlStr  = String.format(API_URL, encoded);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexUS-JavaFX/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("[TranslationService] HTTP " + responseCode);
                return frenchText;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            // Parse JSON manually to avoid heavy library dependency
            // Response: {"responseData":{"translatedText":"..."},...}
            String json = sb.toString();
            String key = "\"translatedText\":\"";
            int start = json.indexOf(key);
            if (start == -1) return frenchText;
            start += key.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return frenchText;

            String translated = json.substring(start, end);
            // Unescape basic HTML entities
            translated = translated
                    .replace("&amp;",  "&")
                    .replace("&lt;",   "<")
                    .replace("&gt;",   ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;",  "'");

            return translated;

        } catch (Exception e) {
            System.err.println("[TranslationService] Error: " + e.getMessage());
            return frenchText; // fallback to original
        }
    }

    /**
     * Translate asynchronously on a background thread, then call onDone on JavaFX thread.
     */
    public static void translateAsync(String text, java.util.function.Consumer<String> onDone) {
        Thread t = new Thread(() -> {
            String result = translateToEnglish(text);
            javafx.application.Platform.runLater(() -> onDone.accept(result));
        });
        t.setDaemon(true);
        t.start();
    }
}