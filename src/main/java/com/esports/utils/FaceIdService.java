package com.esports.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.Base64;
import javax.imageio.ImageIO;

public class FaceIdService {

    private static final int FACE_SIZE = 64;

    /** Center-crops a square from the image as a stand-in for face detection. */
    public static BufferedImage detectAndCropFace(BufferedImage img) {
        if (img == null) return null;
        int w = img.getWidth();
        int h = img.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        BufferedImage cropped = img.getSubimage(x, y, side, side);
        BufferedImage resized = new BufferedImage(FACE_SIZE, FACE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, FACE_SIZE, FACE_SIZE, null);
        g.dispose();
        return resized;
    }

    public static String imageToBase64(BufferedImage img) {
        if (img == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("[FaceId] imageToBase64: " + e.getMessage());
            return null;
        }
    }

    public static BufferedImage base64ToImage(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            System.err.println("[FaceId] base64ToImage: " + e.getMessage());
            return null;
        }
    }

    /** Pearson correlation on grayscale histograms — returns true if correlation > 0.72. */
    public static boolean compareFaces(BufferedImage stored, BufferedImage captured) {
        if (stored == null || captured == null) return false;
        double[] hA = histogram(stored);
        double[] hB = histogram(captured);
        double corr = pearson(hA, hB);
        System.out.println("[FaceId] correlation = " + String.format("%.4f", corr));
        return corr > 0.72;
    }

    private static double[] histogram(BufferedImage img) {
        double[] hist = new double[256];
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b = rgb         & 0xFF;
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                hist[gray]++;
            }
        }
        return hist;
    }

    private static double pearson(double[] a, double[] b) {
        double meanA = 0, meanB = 0;
        for (int i = 0; i < 256; i++) { meanA += a[i]; meanB += b[i]; }
        meanA /= 256; meanB /= 256;
        double num = 0, da = 0, db = 0;
        for (int i = 0; i < 256; i++) {
            double ea = a[i] - meanA, eb = b[i] - meanB;
            num += ea * eb;
            da  += ea * ea;
            db  += eb * eb;
        }
        if (da == 0 || db == 0) return 0;
        return num / Math.sqrt(da * db);
    }

    public static boolean saveFaceData(int userId, String base64Face) {
        String sql = "UPDATE user SET face_data = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, base64Face);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[FaceId] saveFaceData: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeFaceData(int userId) {
        String sql = "UPDATE user SET face_data = NULL WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[FaceId] removeFaceData: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasFaceData(int userId) {
        String sql = "SELECT face_data FROM user WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String fd = rs.getString("face_data");
                return fd != null && !fd.isEmpty();
            }
        } catch (Exception e) {
            System.err.println("[FaceId] hasFaceData: " + e.getMessage());
        }
        return false;
    }

    public static String findEmailByFace(BufferedImage capturedFace) {
        String sql = "SELECT email, face_data FROM user WHERE face_data IS NOT NULL AND is_active = 1";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int checked = 0;
            while (rs.next()) {
                checked++;
                String email = rs.getString("email");
                System.out.println("[FaceId] checking user: " + email);
                BufferedImage stored = base64ToImage(rs.getString("face_data"));
                if (compareFaces(stored, capturedFace)) {
                    System.out.println("[FaceId] MATCH found: " + email);
                    return email;
                }
            }
            System.out.println("[FaceId] no match among " + checked + " enrolled user(s)");
        } catch (Exception e) {
            System.err.println("[FaceId] findEmailByFace: " + e.getMessage());
        }
        return null;
    }
}