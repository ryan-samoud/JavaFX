package com.esports.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudinaryUploadService {
    private static final String CLOUD_NAME = "dqeub6bry";
    private static final String API_KEY = "xxxxxxxxxxxxxxxxx";
    private static final String API_SECRET = "qxxxxxxxxxxxx";
    private static final String UPLOAD_PRESET = "nexusjeu";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String uploadImage(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Image file is missing.");
        }

        long timestamp = System.currentTimeMillis() / 1000L;
        String signatureBase = "timestamp=" + timestamp + "&upload_preset=" + UPLOAD_PRESET;
        String signature = sha1Hex(signatureBase + API_SECRET);

        String boundary = "----NEXUS-CLOUDINARY-" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, file, timestamp, signature);
        String endpoint = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Cloudinary upload failed (" + response.statusCode() + "): " + response.body());
        }

        String secureUrl = extractField(response.body(), "secure_url");
        if (secureUrl.isBlank()) {
            throw new RuntimeException("Cloudinary response missing secure_url: " + response.body());
        }
        return secureUrl;
    }

    private byte[] buildMultipartBody(String boundary, File file, long timestamp, String signature) throws Exception {
        String CRLF = "\r\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writeTextPart(out, boundary, "upload_preset", UPLOAD_PRESET, CRLF);
        writeTextPart(out, boundary, "timestamp", String.valueOf(timestamp), CRLF);
        writeTextPart(out, boundary, "api_key", API_KEY, CRLF);
        writeTextPart(out, boundary, "signature", signature, CRLF);

        String filename = file.getName();
        String mime = detectMimeType(filename);
        out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + mime + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(Files.readAllBytes(file.toPath()));
        out.write(CRLF.getBytes(StandardCharsets.UTF_8));

        out.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void writeTextPart(ByteArrayOutputStream out, String boundary, String name, String value, String crlf) throws Exception {
        out.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        out.write((value + crlf).getBytes(StandardCharsets.UTF_8));
    }

    private String detectMimeType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private String extractField(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String sha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
