package com.esports.service;

import com.esports.model.Jeu;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;

public class JeuPdfService {

    private final QrCodeService qrCodeService = new QrCodeService();

    public void exportJeuCardPdf(Jeu jeu, File outputFile) throws Exception {
        if (jeu == null || outputFile == null) {
            throw new IllegalArgumentException("Jeu and output file are required.");
        }

        Document document = new Document();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            PdfWriter.getInstance(document, fos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            titleFont.setColor(124, 58, 237);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            subFont.setColor(107, 114, 128);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            labelFont.setColor(76, 29, 149);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("NexUS - Fiche Jeu", titleFont);
            title.setSpacingAfter(4);
            document.add(title);
            Paragraph subtitle = new Paragraph("Document genere automatiquement", subFont);
            subtitle.setSpacingAfter(14);
            document.add(subtitle);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.2f, 2.8f});
            infoTable.setSpacingAfter(12);

            addRow(infoTable, "Nom", safe(jeu.getNom(), "Sans nom"), labelFont, valueFont);
            addRow(infoTable, "Categorie",
                    jeu.getCategorie() != null ? safe(jeu.getCategorie().getNomCategorie(), "N/A") : "N/A",
                    labelFont, valueFont);
            addRow(infoTable, "Mode", safe(jeu.getMode(), "N/A"), labelFont, valueFont);
            addRow(infoTable, "Tranche d'age", jeu.getTrancheAge() + "+", labelFont, valueFont);
            addRow(infoTable, "Nombre de joueurs", String.valueOf(jeu.getNbJoueurs()), labelFont, valueFont);
            addRow(infoTable, "Note", String.valueOf(jeu.getNote()), labelFont, valueFont);
            addRow(infoTable, "Description", safe(jeu.getDescription(), "Aucune"), labelFont, valueFont);
            document.add(infoTable);

            byte[] qrBytes = qrCodeService.generateJeuQrPngBytes(jeu, 220);
            if (qrBytes.length > 0) {
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleToFit(180, 180);
                qrImage.setAlignment(Image.ALIGN_LEFT);
                Paragraph qrTitle = new Paragraph("QR Code du jeu", labelFont);
                qrTitle.setSpacingAfter(6);
                document.add(qrTitle);
                document.add(qrImage);
            }
        } catch (DocumentException e) {
            throw new Exception("PDF generation failed: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                    // Ignore close errors.
                }
            }
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBackgroundColor(new java.awt.Color(243, 232, 255));
        c1.setPadding(8);
        c1.setBorderColor(new java.awt.Color(196, 181, 253));
        c1.setBorderWidth(0.8f);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBackgroundColor(new java.awt.Color(250, 245, 255));
        c2.setPadding(8);
        c2.setBorderColor(new java.awt.Color(221, 214, 254));
        c2.setBorderWidth(0.8f);

        table.addCell(c1);
        table.addCell(c2);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
