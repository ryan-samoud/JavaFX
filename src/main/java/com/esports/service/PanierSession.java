package com.esports.service;

import java.util.UUID;

public class PanierSession {
    private static String sessionId = null;
    private static int panierId = 0;

    public static String getSessionId() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            System.out.println("[SESSION] Nouvelle session: " + sessionId);
        }
        return sessionId;
    }

    public static int getPanierId() { return panierId; }
    public static void setPanierId(int id) { panierId = id; }

    public static void reset() {
        sessionId = null;
        panierId = 0;
    }
}