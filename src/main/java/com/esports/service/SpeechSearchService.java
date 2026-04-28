package com.esports.service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class SpeechSearchService {
    public static final String DEEPGRAM_API_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String ELEVENLABS_API_KEY = "wxxxxxxxxxxxxxxxx";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public static class MicRecordingSession {
        private final File file;
        private final TargetDataLine line;
        private final Thread writerThread;

        private MicRecordingSession(File file, TargetDataLine line, Thread writerThread) {
            this.file = file;
            this.line = line;
            this.writerThread = writerThread;
        }
    }

    public MicRecordingSession startMicrophoneRecording() throws Exception {
        AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Microphone line is not supported on this machine.");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        File wavFile = File.createTempFile("nexus-voice-search-", ".wav");
        wavFile.deleteOnExit();

        Thread writer = new Thread(() -> {
            try (AudioInputStream stream = new AudioInputStream(line)) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavFile);
            } catch (Exception ignored) {
                // Ignore write interruption when line stops.
            }
        }, "mic-recorder-writer");
        writer.setDaemon(true);
        writer.start();

        return new MicRecordingSession(wavFile, line, writer);
    }

    public File stopMicrophoneRecording(MicRecordingSession session) throws Exception {
        if (session == null) {
            throw new IllegalArgumentException("No active recording session.");
        }
        session.line.stop();
        session.line.close();
        session.writerThread.join(2000);
        return session.file;
    }

    public String transcribeWithDeepgram(File audioFile) throws Exception {
        if (audioFile == null || !audioFile.exists()) {
            throw new IllegalArgumentException("Audio file is missing.");
        }

        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        String contentType = detectAudioContentType(audioFile.getName());
        String url = "https://api.deepgram.com/v1/listen?model=nova-2&smart_format=true&language=fr";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Token " + DEEPGRAM_API_KEY)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Deepgram error (" + response.statusCode() + "): " + response.body());
        }

        String transcript = extractFirstTranscript(response.body());
        if (transcript.isBlank()) {
            throw new RuntimeException("No speech recognized in audio.");
        }
        return transcript;
    }

    public byte[] synthesizeWithElevenLabs(String text) throws Exception {
        if (text == null || text.isBlank()) {
            return new byte[0];
        }
        String voiceId = "EXAVITQu4vr4xnSDxMaL";
        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId;
        String payload = "{\"text\":\"" + escapeJson(text) + "\",\"model_id\":\"eleven_multilingual_v2\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("xi-api-key", ELEVENLABS_API_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("ElevenLabs error (" + response.statusCode() + ")");
        }
        return response.body();
    }

    private String detectAudioContentType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".webm")) return "audio/webm";
        return "application/octet-stream";
    }

    private String extractFirstTranscript(String json) {
        if (json == null || json.isBlank()) return "";
        Pattern p = Pattern.compile("\"transcript\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String candidate = unescapeJson(m.group(1)).trim();
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
