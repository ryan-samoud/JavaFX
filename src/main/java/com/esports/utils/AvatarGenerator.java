package com.esports.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AvatarGenerator {

    private static final int SIZE = 256;

    // ── Skin tones ────────────────────────────────────────────────────
    private static final Color[] SKIN = {
        new Color(255, 219, 172),  // light
        new Color(241, 194, 125),  // medium-light
        new Color(198, 134,  66),  // medium
        new Color(141,  85,  36),  // medium-dark
        new Color( 90,  55,  25),  // dark
        new Color(255, 200, 160),  // peach
    };

    // ── Hair colors ───────────────────────────────────────────────────
    private static final Color[] HAIR = {
        new Color( 20,  14,  10),  // black
        new Color( 80,  45,  20),  // dark-brown
        new Color(155,  95,  35),  // brown
        new Color(205, 165,  60),  // blonde
        new Color(185,  60,  40),  // auburn
        new Color(100,  75, 185),  // purple
        new Color( 45, 100, 200),  // blue
        new Color(220, 220, 220),  // silver
        new Color(230, 100,  40),  // orange
    };

    // ── Eye colors ────────────────────────────────────────────────────
    private static final Color[] EYES = {
        new Color( 75,  50,  25),  // brown
        new Color( 55,  95, 175),  // blue
        new Color( 60, 130,  70),  // green
        new Color(100,  80,  35),  // hazel
        new Color(105, 115, 125),  // gray
        new Color(160, 110,  30),  // amber
    };

    // ── Background palettes [top, bottom] ────────────────────────────
    private static final Color[][] BG = {
        {new Color(109, 40, 217), new Color(55, 20, 130)},   // violet
        {new Color( 37, 99, 235), new Color(17, 50, 140)},   // blue
        {new Color(  5,150,105), new Color(  3, 80,  60)},   // emerald
        {new Color(220, 38, 38), new Color(120, 15, 15)},    // red
        {new Color(217, 119, 6), new Color(130, 60,  5)},    // amber
        {new Color(219, 39,119), new Color(130, 18, 70)},    // pink
        {new Color(  8,145,178), new Color(  4, 80,110)},    // cyan
        {new Color( 67, 56,202), new Color( 30, 25,120)},    // indigo
        {new Color(  4,120, 87), new Color(  2, 65, 50)},    // dark-green
        {new Color(157, 23, 77), new Color( 90, 10, 45)},    // rose
    };

    // ── Shirt colors ──────────────────────────────────────────────────
    private static final Color[] SHIRTS = {
        new Color( 55,  65,  81),  // dark-gray
        new Color( 30,  58, 138),  // navy
        new Color( 88,  28, 135),  // dark-purple
        new Color(  7,  89,  133), // dark-cyan
        new Color(127,  29,  29),  // dark-red
        new Color( 20,  83,  45),  // dark-green
        new Color( 17,  24,  39),  // almost-black
        new Color(120,  53,  15),  // dark-orange
    };

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────

    public static String generateAndSave(int userId, String nom, String prenom) {
        try {
            BufferedImage img = generate(nom, prenom);
            String dir  = System.getProperty("user.home") + "/.nexus_avatars";
            Files.createDirectories(Paths.get(dir));
            String path = dir + "/" + userId + ".png";
            ImageIO.write(img, "png", new File(path));
            return path;
        } catch (IOException e) {
            System.err.println("[Avatar] save error: " + e.getMessage());
            return null;
        }
    }

    public static BufferedImage generate(String nom, String prenom) {
        long h = hash53((prenom + nom).toLowerCase().replaceAll("[^a-z0-9]", ""));

        // pick features deterministically
        Color skin    = SKIN [(int)(Math.abs(h       ) % SKIN.length)];
        Color hair    = HAIR [(int)(Math.abs(h >>  4 ) % HAIR.length)];
        Color eyeCol  = EYES [(int)(Math.abs(h >>  8 ) % EYES.length)];
        Color[] bg    = BG   [(int)(Math.abs(h >> 12 ) % BG.length)];
        Color shirt   = SHIRTS[(int)(Math.abs(h >> 16) % SHIRTS.length)];
        int   hairStyle  = (int)(Math.abs(h >> 20) % 5);   // 0-4
        int   mouthStyle = (int)(Math.abs(h >> 24) % 3);   // 0-2
        boolean glasses  = ((h >> 28) & 1L) == 1;

        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        hint(g);

        // ── Clip to rounded square ────────────────────────────────────
        g.setClip(new RoundRectangle2D.Float(0, 0, SIZE, SIZE, 56, 56));

        // ── Background gradient ───────────────────────────────────────
        g.setPaint(new GradientPaint(0, 0, bg[0], SIZE, SIZE, bg[1]));
        g.fillRect(0, 0, SIZE, SIZE);

        // ── Shirt / body ──────────────────────────────────────────────
        drawShirt(g, shirt, skin);

        // ── Neck ──────────────────────────────────────────────────────
        g.setColor(skin);
        g.fillRoundRect(103, 185, 50, 35, 14, 14);

        // ── Head ─────────────────────────────────────────────────────
        g.setColor(skin);
        g.fillOval(38, 30, 180, 196);

        // ── Ears ──────────────────────────────────────────────────────
        g.setColor(skin);
        g.fillOval(28,  95, 24, 32);   // left
        g.fillOval(204, 95, 24, 32);   // right
        // inner ear shading
        g.setColor(skin.darker());
        g.fillOval(33, 100, 13, 20);
        g.fillOval(210, 100, 13, 20);

        // ── Hair ─────────────────────────────────────────────────────
        drawHair(g, hairStyle, hair, h);

        // ── Eyebrows ─────────────────────────────────────────────────
        drawEyebrows(g, hair);

        // ── Eyes ─────────────────────────────────────────────────────
        drawEye(g, 76, 115, eyeCol);    // left eye
        drawEye(g, 152, 115, eyeCol);   // right eye

        // ── Nose ─────────────────────────────────────────────────────
        drawNose(g, skin);

        // ── Mouth ─────────────────────────────────────────────────────
        drawMouth(g, mouthStyle, skin);

        // ── Glasses (optional) ────────────────────────────────────────
        if (glasses) drawGlasses(g);

        // ── Face contour shadow ───────────────────────────────────────
        g.setColor(new Color(0, 0, 0, 18));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(38, 30, 180, 196);

        g.dispose();
        return img;
    }

    // ─────────────────────────────────────────────────────────────────
    // BODY / SHIRT
    // ─────────────────────────────────────────────────────────────────
    private static void drawShirt(Graphics2D g, Color shirt, Color skin) {
        // body shape
        g.setColor(shirt);
        int[] xPts = {0,   70,  90, 166, 186, SIZE, SIZE, 0};
        int[] yPts = {SIZE, 210, 200, 200, 210, SIZE, SIZE, SIZE};
        g.fillPolygon(xPts, yPts, xPts.length);

        // collar
        g.setColor(shirt.brighter());
        int[] cx = {90, 128, 166};
        int[] cy = {200, 216, 200};
        g.fillPolygon(cx, cy, 3);

        // collar outline
        g.setColor(shirt.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(cx, cy, 3);
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────
    // HAIR
    // ─────────────────────────────────────────────────────────────────
    private static void drawHair(Graphics2D g, int style, Color hair, long h) {
        g.setColor(hair);
        switch (style) {
            case 0 -> { // short / crew cut
                g.fillArc(38, 24, 180, 130, 0, 180);
                g.fillRect(38, 30, 180, 55);
            }
            case 1 -> { // long — sides flow down
                g.fillArc(34, 22, 188, 132, 0, 180);
                g.fillRect(34, 30, 22, 130);   // left side
                g.fillRect(200, 30, 22, 130);  // right side
                // rounded bottom of side hair
                g.fillOval(24, 130, 35, 50);
                g.fillOval(197, 130, 35, 50);
            }
            case 2 -> { // spiky
                g.fillArc(38, 28, 180, 110, 0, 180);
                // spikes
                int[] sx = {68,80,90, 108,118,128, 148,158,168, 178,188};
                int[] sy = { 5,35,5,    5, 35,  5,   5, 35,  5,   5, 35};
                for (int i = 0; i < sx.length - 2; i += 2) {
                    int[] px = {sx[i], sx[i+1], sx[i+2]};
                    int[] py = {sy[i], sy[i+1], sy[i+2]};
                    g.fillPolygon(px, py, 3);
                }
            }
            case 3 -> { // wavy / medium
                g.fillArc(36, 22, 184, 128, 0, 180);
                // wavy side locks
                g.fillOval(30, 85, 28, 60);
                g.fillOval(198, 85, 28, 60);
                g.fillOval(28, 130, 28, 40);
                g.fillOval(200, 130, 28, 40);
            }
            case 4 -> { // afro / voluminous
                g.fillOval(18, 5, 220, 170);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // EYEBROWS
    // ─────────────────────────────────────────────────────────────────
    private static void drawEyebrows(Graphics2D g, Color hairCol) {
        Color brow = hairCol.darker();
        g.setColor(brow);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // left brow — slight arch
        g.drawArc(66, 97, 40, 18, 15, 150);
        // right brow
        g.drawArc(150, 97, 40, 18, 15, 150);
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────
    // EYES
    // ─────────────────────────────────────────────────────────────────
    private static void drawEye(Graphics2D g, int cx, int cy, Color iris) {
        // white sclera
        g.setColor(Color.WHITE);
        g.fillOval(cx - 18, cy - 13, 36, 26);
        // iris
        g.setColor(iris);
        g.fillOval(cx - 11, cy - 11, 22, 22);
        // pupil
        g.setColor(new Color(20, 12, 8));
        g.fillOval(cx - 6, cy - 6, 12, 12);
        // highlight
        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(cx - 2, cy - 8, 5, 5);
        // outline
        g.setColor(new Color(0, 0, 0, 60));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx - 18, cy - 13, 36, 26);
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────
    // NOSE
    // ─────────────────────────────────────────────────────────────────
    private static void drawNose(Graphics2D g, Color skin) {
        g.setColor(skin.darker());
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // simple curved nose bridge + nostrils
        Path2D nose = new Path2D.Float();
        nose.moveTo(122, 138);
        nose.curveTo(116, 152, 112, 162, 116, 168);
        nose.curveTo(120, 172, 136, 172, 140, 168);
        nose.curveTo(144, 162, 140, 152, 134, 138);
        g.draw(nose);
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────
    // MOUTH
    // ─────────────────────────────────────────────────────────────────
    private static void drawMouth(Graphics2D g, int style, Color skin) {
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (style) {
            case 0 -> { // gentle smile
                g.setColor(new Color(160, 80, 80));
                g.drawArc(96, 174, 64, 28, 200, 140);
                // teeth hint
                g.setColor(Color.WHITE);
                g.fillArc(100, 176, 56, 20, 200, 140);
                g.setColor(new Color(200, 140, 140));
                g.drawArc(96, 174, 64, 28, 200, 140);
            }
            case 1 -> { // big smile
                g.setColor(new Color(160, 80, 80));
                g.drawArc(88, 170, 80, 36, 200, 140);
                g.setColor(Color.WHITE);
                g.fillArc(92, 174, 72, 26, 200, 140);
                g.setColor(new Color(200, 140, 140));
                g.drawArc(88, 170, 80, 36, 200, 140);
            }
            case 2 -> { // neutral / slight smirk
                g.setColor(new Color(160, 80, 80));
                g.drawLine(104, 184, 152, 182);
                g.drawArc(140, 178, 20, 12, 220, 120);
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────
    // GLASSES
    // ─────────────────────────────────────────────────────────────────
    private static void drawGlasses(Graphics2D g) {
        g.setColor(new Color(40, 40, 40, 200));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // left lens
        g.drawRoundRect(62, 108, 48, 28, 12, 12);
        // right lens
        g.drawRoundRect(146, 108, 48, 28, 12, 12);
        // bridge
        g.drawLine(110, 120, 146, 120);
        // temple arms
        g.drawLine(62, 118, 38, 112);
        g.drawLine(194, 118, 218, 112);
        g.setStroke(new BasicStroke(1f));
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────
    private static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
    }

    private static long hash53(String s) {
        long h1 = 0xDEADBEEFL, h2 = 0xCAFEBABEL;
        for (char c : s.toCharArray()) {
            h1 = (h1 ^ c) * 0x9e3779b9L;
            h2 = (h2 ^ c) * 0x517cc1b727220a95L;
        }
        return h1 ^ h2;
    }
}