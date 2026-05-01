package com.esports.utils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class CartoonAvatarGenerator {

    private static final int OUT = 256;

    // frame gradient pairs [ring-color, glow-color]
    private static final int[][] FRAMES = {
        {0xFF7C3AED, 0xFFA78BFA},
        {0xFF2563EB, 0xFF60A5FA},
        {0xFFDB2777, 0xFFF472B6},
        {0xFF059669, 0xFF34D399},
        {0xFFD97706, 0xFFFBBF24},
        {0xFF0891B2, 0xFF22D3EE},
        {0xFF4338CA, 0xFF818CF8},
        {0xFFDC2626, 0xFFF87171},
        {0xFF047857, 0xFF6EE7B7},
        {0xFF9D174D, 0xFFFB7185},
    };

    /**
     * Turns a real photo into a cartoon avatar.
     * @param source   the user's photo (any size)
     * @param variation 0-9, controls frame color + saturation + posterization level
     */
    public static BufferedImage cartoonify(BufferedImage source, int variation) {
        // 1. center-crop to square then resize to working size
        BufferedImage sq     = centerCrop(source);
        BufferedImage sized  = resize(sq, 220, 220);

        // 2. smooth (mimics bilateral — 3 passes of box blur)
        BufferedImage blurred = blur(sized, 3);

        // 3. posterize (flat cartoon colors) — level varies with variation
        int levels = 4 + (variation % 3);           // 4, 5 or 6
        BufferedImage flat = posterize(blurred, levels);

        // 4. saturation boost (vibrant cartoon look)
        float sat = 1.25f + (variation % 5) * 0.08f; // 1.25 → 1.57
        BufferedImage vivid = boostSaturation(flat, sat);

        // 5. edge map from original (dark outlines)
        BufferedImage edges = detectEdges(sized);

        // 6. compose final 256×256 image
        int[]  frame = FRAMES[variation % FRAMES.length];
        Color  ring  = new Color(frame[0], true);
        Color  glow  = new Color(frame[1], true);

        BufferedImage result = new BufferedImage(OUT, OUT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        hint(g);

        // background gradient
        g.setPaint(new GradientPaint(0, 0, darken(ring, 0.5f), OUT, OUT, darken(ring, 0.25f)));
        g.fillRect(0, 0, OUT, OUT);

        // photo clipped to circle
        Shape circle = new Ellipse2D.Float(14, 14, 228, 228);
        g.setClip(circle);
        g.drawImage(vivid, 18, 18, 220, 220, null);

        // edge overlay (dark outlines on top)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g.drawImage(edges, 18, 18, 220, 220, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g.setClip(null);

        // outer glow ring
        g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 90));
        g.setStroke(new BasicStroke(10f));
        g.drawOval(5, 5, 246, 246);

        // crisp inner ring
        g.setColor(glow);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(13, 13, 230, 230);

        g.dispose();
        return result;
    }

    // ── image processing ─────────────────────────────────────────────

    private static BufferedImage centerCrop(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int side = Math.min(w, h);
        return img.getSubimage((w - side) / 2, (h - side) / 2, side, side);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage blur(BufferedImage img, int passes) {
        float v = 1f / 9f;
        float[] k = {v,v,v, v,v,v, v,v,v};
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, k), ConvolveOp.EDGE_NO_OP, null);
        // ConvolveOp needs TYPE_INT_RGB
        BufferedImage src = toRGB(img);
        for (int i = 0; i < passes; i++) src = op.filter(src, null);
        return src;
    }

    private static BufferedImage posterize(BufferedImage img, int levels) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        float step = 255f / (levels - 1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = clamp(Math.round(((rgb >> 16) & 0xFF) / step) * (int) step);
                int g = clamp(Math.round(((rgb >>  8) & 0xFF) / step) * (int) step);
                int b = clamp(Math.round(( rgb        & 0xFF) / step) * (int) step);
                out.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private static BufferedImage boostSaturation(BufferedImage img, float factor) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        float[] hsb = new float[3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                hsb[1] = Math.min(1f, hsb[1] * factor);
                out.setRGB(x, y, Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
            }
        }
        return out;
    }

    private static BufferedImage detectEdges(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] gray = new int[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                gray[y * w + x] = (int)(0.299*((rgb>>16)&0xFF) + 0.587*((rgb>>8)&0xFF) + 0.114*(rgb&0xFF));
            }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int gx = -gray[(y-1)*w+(x-1)] - 2*gray[y*w+(x-1)] - gray[(y+1)*w+(x-1)]
                        + gray[(y-1)*w+(x+1)] + 2*gray[y*w+(x+1)] + gray[(y+1)*w+(x+1)];
                int gy = -gray[(y-1)*w+(x-1)] - 2*gray[(y-1)*w+x] - gray[(y-1)*w+(x+1)]
                        + gray[(y+1)*w+(x-1)] + 2*gray[(y+1)*w+x] + gray[(y+1)*w+(x+1)];
                int mag = clamp((int) Math.sqrt((double)gx*gx + (double)gy*gy));
                if (mag > 45) {
                    int d = clamp(255 - mag * 2);
                    out.setRGB(x, y, (180 << 24) | (d << 16) | (d << 8) | d);
                } else {
                    out.setRGB(x, y, 0x00FFFFFF);
                }
            }
        }
        return out;
    }

    // ── helpers ───────────────────────────────────────────────────────

    private static BufferedImage toRGB(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_RGB) return img;
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    private static Color darken(Color c, float f) {
        return new Color(
            Math.max(0, (int)(c.getRed()   * f)),
            Math.max(0, (int)(c.getGreen() * f)),
            Math.max(0, (int)(c.getBlue()  * f)));
    }

    private static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}