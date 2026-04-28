package com.esports.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeSearchService {
    public static class YoutubeVideo {
        private final String videoId;
        private final String title;
        private final String url;
        private final String thumbnailUrl;

        public YoutubeVideo(String videoId, String title) {
            this.videoId = videoId;
            this.title = title;
            this.url = "https://www.youtube.com/watch?v=" + videoId;
            this.thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
        }

        public String getVideoId() { return videoId; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getThumbnailUrl() { return thumbnailUrl; }
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public List<YoutubeVideo> searchVideos(String gameName, int limit) {
        try {
            String query = URLEncoder.encode(gameName + " gameplay live", StandardCharsets.UTF_8);
            String url = "https://www.youtube.com/results?search_query=" + query;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return parseVideoIds(response.body(), Math.max(1, Math.min(limit, 8)));
        } catch (Exception e) {
            System.err.println("[YoutubeSearchService] " + e.getMessage());
            return List.of();
        }
    }

    private List<YoutubeVideo> parseVideoIds(String html, int limit) {
        Pattern idPattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"");
        Matcher matcher = idPattern.matcher(html == null ? "" : html);
        Set<String> uniqueIds = new LinkedHashSet<>();
        while (matcher.find() && uniqueIds.size() < limit) {
            uniqueIds.add(matcher.group(1));
        }

        List<YoutubeVideo> videos = new ArrayList<>();
        int idx = 1;
        for (String id : uniqueIds) {
            videos.add(new YoutubeVideo(id, "YouTube video #" + idx++));
        }
        return videos;
    }
}
