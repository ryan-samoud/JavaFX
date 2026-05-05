package com.esports.utils;

import com.esports.model.Evenement;
import com.esports.model.Sponsor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UTILS — EvenementPdfExporter.java
 * Generates a styled PDF using Apache PDFBox 2.
 * No module-info conflicts.
 */
public class EvenementPdfExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Colors
    private static final Color COL_BG       = new Color(17,  11,  40);
    private static final Color COL_CARD     = new Color(26,  16,  56);
    private static final Color COL_PURPLE   = new Color(124, 58,  237);
    private static final Color COL_PINK     = new Color(236, 72,  153);
    private static final Color COL_TEXT     = new Color(226, 232, 240);
    private static final Color COL_MUTED    = new Color(148, 163, 184);
    private static final Color COL_GREEN    = new Color(74,  222, 128);
    private static final Color COL_RED      = new Color(248, 113, 113);
    private static final Color COL_YELLOW   = new Color(251, 191, 36);
    private static final Color COL_ACCENT   = new Color(192, 132, 252);
    private static final Color COL_DARKCARD = new Color(40,  28,  70);

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN  = 40f;

    public static boolean exportToPdf(Evenement ev, List<Sponsor> sponsors, String filePath) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_H - MARGIN;

                // ── Background ──
                fillRect(cs, 0, 0, PAGE_W, PAGE_H, COL_BG);

                // ── Header bar ──
                fillRect(cs, 0, PAGE_H - 90, PAGE_W * 0.65f, 90, COL_PURPLE);
                fillRect(cs, PAGE_W * 0.65f, PAGE_H - 90, PAGE_W * 0.35f, 90, COL_PINK);

                // Header text — event name
                String nom = ev.getNom() != null ? clean(ev.getNom().toUpperCase()) : "EVENEMENT";
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.setNonStrokingColor(Color.WHITE);
                cs.newLineAtOffset(MARGIN, PAGE_H - 55);
                cs.showText(truncate(nom, 36));
                cs.endText();

                // Status badge
                boolean past = ev.isPast();
                String status = past ? "TERMINE" : "A VENIR";
                Color badgeColor = past ? COL_RED : COL_GREEN;
                fillRect(cs, PAGE_W - 130, PAGE_H - 45, 90, 22, badgeColor);
                drawText(cs, status, PDType1Font.HELVETICA_BOLD, 9, Color.WHITE, PAGE_W - 120, PAGE_H - 38);

                // Separator line
                cs.setStrokingColor(COL_PURPLE);
                cs.setLineWidth(1.5f);
                cs.moveTo(MARGIN, PAGE_H - 95);
                cs.lineTo(PAGE_W - MARGIN, PAGE_H - 95);
                cs.stroke();

                y = PAGE_H - 115;

                // ── INFO SECTION ──
                y = drawSectionTitle(cs, "INFORMATIONS", y);
                y -= 8;

                float infoLeft  = MARGIN;
                float infoRight = MARGIN + 260;
                float infoW1    = 120, infoW2 = 260, rowH = 28;

                String[] labels = {"Date",      "Lieu",          "Organisateur",      "ID Evenement"};
                String[] values = {
                        ev.getDate() != null ? ev.getDate().format(DATE_FMT) : "-",
                        ev.getLieu() != null ? clean(truncate(ev.getLieu(), 40)) : "-",
                        "NexUS Gaming Arena",
                        "#" + ev.getId()
                };

                for (int i = 0; i < labels.length; i++) {
                    fillRect(cs, infoLeft, y - rowH, infoW1, rowH, COL_CARD);
                    fillRect(cs, infoLeft + infoW1, y - rowH, infoW2, rowH, COL_CARD);
                    drawBorder(cs, infoLeft, y - rowH, infoW1 + infoW2, rowH);
                    drawText(cs, labels[i], PDType1Font.HELVETICA_BOLD, 10, COL_MUTED, infoLeft + 6, y - 18);
                    drawText(cs, values[i], PDType1Font.HELVETICA, 10, COL_TEXT, infoLeft + infoW1 + 6, y - 18);
                    y -= rowH;
                }
                y -= 14;

                // ── STATS SECTION ──
                y = drawSectionTitle(cs, "STATISTIQUES", y);
                y -= 8;

                double totalBudget = sponsors != null
                        ? sponsors.stream().mapToDouble(Sponsor::getPrix).sum() : 0;

                String[] statVals = {
                        String.valueOf(ev.getNbrParticipant()),
                        String.valueOf(sponsors != null ? sponsors.size() : 0),
                        String.format("%.0f EUR", totalBudget)
                };
                String[] statLabels = {"PARTICIPANTS", "SPONSORS", "BUDGET"};

                float statW = (PAGE_W - 2 * MARGIN) / 3;
                float statH = 55;
                for (int i = 0; i < 3; i++) {
                    float sx = MARGIN + i * statW;
                    fillRect(cs, sx, y - statH, statW, statH, COL_CARD);
                    drawBorderColor(cs, sx, y - statH, statW, statH, COL_PURPLE);
                    // Big number
                    drawTextCentered(cs, statVals[i], PDType1Font.HELVETICA_BOLD, 20, COL_ACCENT,
                            sx, y - statH, statW, statH - 18);
                    // Label
                    drawTextCentered(cs, statLabels[i], PDType1Font.HELVETICA_BOLD, 8, COL_MUTED,
                            sx, y - statH, statW, 14);
                }
                y -= statH + 14;

                // ── DESCRIPTION ──
                if (ev.getDescription() != null && !ev.getDescription().isBlank()) {
                    y = drawSectionTitle(cs, "DESCRIPTION", y);
                    y -= 8;

                    String desc = clean(ev.getDescription());
                    List<String> lines = wrapText(desc, 80);
                    float descH = lines.size() * 14f + 20;

                    fillRect(cs, MARGIN, y - descH, PAGE_W - 2 * MARGIN, descH, COL_CARD);
                    // Purple left border
                    fillRect(cs, MARGIN, y - descH, 3, descH, COL_PURPLE);

                    float ly = y - 14;
                    for (String line : lines) {
                        drawText(cs, line, PDType1Font.HELVETICA, 10, COL_TEXT, MARGIN + 10, ly);
                        ly -= 14;
                    }
                    y -= descH + 14;
                }

                // ── SPONSORS ──
                if (sponsors != null && !sponsors.isEmpty()) {
                    y = drawSectionTitle(cs, "SPONSORS PARTENAIRES", y);
                    y -= 8;

                    float[] colWidths = {150, 80, 150, 80};
                    String[] headers  = {"NOM", "TYPE", "EMAIL", "BUDGET"};
                    float tableW = PAGE_W - 2 * MARGIN;
                    float colH   = 24f;

                    // Header row
                    float hx = MARGIN;
                    for (int i = 0; i < headers.length; i++) {
                        fillRect(cs, hx, y - colH, colWidths[i], colH, COL_DARKCARD);
                        drawBorder(cs, hx, y - colH, colWidths[i], colH);
                        drawText(cs, headers[i], PDType1Font.HELVETICA_BOLD, 9, COL_MUTED, hx + 5, y - 15);
                        hx += colWidths[i];
                    }
                    y -= colH;

                    // Data rows
                    double total = 0;
                    for (Sponsor s : sponsors) {
                        String[] cells = {
                                clean(truncate(s.getNom() != null ? s.getNom() : "-", 22)),
                                s.getTypeLabel(),
                                clean(truncate(s.getEmail() != null ? s.getEmail() : "-", 24)),
                                String.format("%.0f EUR", s.getPrix())
                        };
                        float rx = MARGIN;
                        for (int i = 0; i < cells.length; i++) {
                            fillRect(cs, rx, y - colH, colWidths[i], colH, COL_CARD);
                            drawBorder(cs, rx, y - colH, colWidths[i], colH);
                            drawText(cs, cells[i], PDType1Font.HELVETICA, 9, COL_TEXT, rx + 5, y - 15);
                            rx += colWidths[i];
                        }
                        total += s.getPrix();
                        y -= colH;
                    }

                    // Total row
                    fillRect(cs, MARGIN, y - colH, tableW - 80, colH, COL_DARKCARD);
                    fillRect(cs, MARGIN + tableW - 80, y - colH, 80, colH, COL_DARKCARD);
                    drawBorder(cs, MARGIN, y - colH, tableW - 80, colH);
                    drawBorder(cs, MARGIN + tableW - 80, y - colH, 80, colH);
                    drawText(cs, "TOTAL", PDType1Font.HELVETICA_BOLD, 9, COL_MUTED,
                            MARGIN + tableW - 160, y - 15);
                    drawText(cs, String.format("%.0f EUR", total), PDType1Font.HELVETICA_BOLD, 9,
                            COL_GREEN, MARGIN + tableW - 75, y - 15);
                    y -= colH + 14;
                }

                // ── FOOTER ──
                cs.setStrokingColor(COL_CARD);
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, MARGIN + 20);
                cs.lineTo(PAGE_W - MARGIN, MARGIN + 20);
                cs.stroke();

                String footer = "Document officiel NexUS Gaming Arena - Genere le " +
                        java.time.LocalDate.now().format(DATE_FMT);
                drawTextCentered(cs, footer, PDType1Font.HELVETICA_OBLIQUE, 8, COL_MUTED,
                        0, MARGIN, PAGE_W, 16);
            }

            doc.save(new File(filePath));
            return true;

        } catch (Exception e) {
            System.err.println("[EvenementPdfExporter] ERROR: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── Drawing helpers ───────────────────────────────────────────

    private static void fillRect(PDPageContentStream cs, float x, float y,
                                 float w, float h, Color c) throws Exception {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void drawBorder(PDPageContentStream cs, float x, float y,
                                   float w, float h) throws Exception {
        cs.setStrokingColor(new Color(55, 48, 80));
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void drawBorderColor(PDPageContentStream cs, float x, float y,
                                        float w, float h, Color c) throws Exception {
        cs.setStrokingColor(c);
        cs.setLineWidth(1f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void drawText(PDPageContentStream cs, String text,
                                 PDType1Font font, float size, Color color,
                                 float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static float drawSectionTitle(PDPageContentStream cs, String text, float y)
            throws Exception {
        // Purple accent bar
        fillRect(cs, MARGIN, y - 18, 3, 16, COL_PURPLE);
        drawText(cs, text, PDType1Font.HELVETICA_BOLD, 12, COL_ACCENT, MARGIN + 8, y - 14);
        return y - 22;
    }

    private static void drawTextCentered(PDPageContentStream cs, String text,
                                         PDType1Font font, float size, Color color,
                                         float boxX, float boxY, float boxW, float boxH)
            throws Exception {
        float textW = font.getStringWidth(text) / 1000 * size;
        float tx = boxX + (boxW - textW) / 2;
        float ty = boxY + (boxH - size) / 2 + 2;
        drawText(cs, text, font, size, color, tx, ty);
    }

    // ── Text utilities ────────────────────────────────────────────

    /** Remove non-latin1 characters that PDFBox Type1 fonts can't render */
    private static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 256) sb.append(c);
            else {
                // Replace common French accented chars
                switch (c) {
                    case 'é','è','ê','ë' -> sb.append('e');
                    case 'à','â','ä'     -> sb.append('a');
                    case 'ù','û','ü'     -> sb.append('u');
                    case 'î','ï'         -> sb.append('i');
                    case 'ô','ö'         -> sb.append('o');
                    case 'ç'             -> sb.append('c');
                    case 'É','È','Ê'     -> sb.append('E');
                    case 'À','Â'         -> sb.append('A');
                    case 'Ù','Û'         -> sb.append('U');
                    case 'Î'             -> sb.append('I');
                    case 'Ô'             -> sb.append('O');
                    case 'Ç'             -> sb.append('C');
                    default              -> sb.append('?');
                }
            }
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }

    private static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > maxChars) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) lines.add(line.toString().trim());
        return lines;
    }
}