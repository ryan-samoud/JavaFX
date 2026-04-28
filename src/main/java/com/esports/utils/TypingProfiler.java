package com.esports.utils;

import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Attaches to a PasswordField and records inter-key timing intervals.
 * Call attach() in initialize(), getIntervals() after the field is filled.
 */
public class TypingProfiler {

    private final List<Long> timestamps = new ArrayList<>();

    public void attach(PasswordField field) {
        field.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            // ignore modifier-only events (Shift, Ctrl, etc.)
            String ch = e.getCharacter();
            if (ch == null || ch.isEmpty() || ch.charAt(0) < 32) return;
            timestamps.add(System.currentTimeMillis());
        });
    }

    /**
     * Returns speed-normalized inter-key intervals.
     * Dividing by the mean makes the vector rhythm-only (fast vs slow typist irrelevant).
     */
    public List<Double> getIntervals() {
        if (timestamps.size() < 3) return List.of();

        List<Double> raw = new ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            double gap = timestamps.get(i) - timestamps.get(i - 1);
            if (gap < 2000) raw.add(gap);   // ignore pauses > 2 s (user hesitated)
        }
        if (raw.size() < 2) return List.of();

        double mean = raw.stream().mapToDouble(d -> d).average().orElse(1);
        if (mean < 1) mean = 1;
        List<Double> norm = new ArrayList<>();
        for (double v : raw) norm.add(v / mean);
        return norm;
    }

    public void reset() { timestamps.clear(); }
}