package com.esports.service;

import com.esports.model.Tournament;
import com.esports.model.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;

public class TicketService {

    private static final String TICKETS_DIR = "tickets";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public String generateBoardingPass(User user, Tournament tournament) {
        try {
            // 1. Create directory if not exists
            File dir = new File(TICKETS_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fileName = TICKETS_DIR + "/Ticket_" + tournament.getId() + "_" + user.getId() + ".pdf";
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);
            // Boarding pass size is roughly 2:1 ratio
            PageSize pageSize = new PageSize(600, 250);
            pdf.setDefaultPageSize(pageSize);
            Document document = new Document(pdf);
            document.setMargins(0, 0, 0, 0);

            // Colors
            DeviceRgb darkBg = new DeviceRgb(20, 17, 43);   // #14112b
            DeviceRgb accent  = new DeviceRgb(124, 58, 237); // #7c3aed (Violet)
            DeviceRgb gray    = new DeviceRgb(124, 111, 168);

            // Main Table (Container)
            Table mainTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                    .useAllAvailableWidth()
                    .setHeight(250)
                    .setBackgroundColor(darkBg);

            // --- LEFT SIDE: Information ---
            Cell infoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(30);
            
            Paragraph logo = new Paragraph("NEXUS ESPORTS")
                    .setFontColor(accent)
                    .setBold()
                    .setFontSize(18);
            infoCell.add(logo);

            infoCell.add(new Paragraph("BOARDING PASS - TOURNAMENT ENTRY")
                    .setFontColor(ColorConstants.WHITE)
                    .setBold()
                    .setFontSize(12)
                    .setMarginTop(10));

            // Grid for data
            Table dataGrid = new Table(2).useAllAvailableWidth().setMarginTop(20);
            
            dataGrid.addCell(labelCell("PLAYER", gray));
            dataGrid.addCell(labelCell("TOURNAMENT", gray));
            
            dataGrid.addCell(valueCell(user.getNom().toUpperCase() + " " + user.getPrenom().toUpperCase(), ColorConstants.WHITE));
            dataGrid.addCell(valueCell(tournament.getNom(), ColorConstants.WHITE));

            dataGrid.addCell(labelCell("GAME", gray));
            dataGrid.addCell(labelCell("DATE & TIME", gray));

            dataGrid.addCell(valueCell(tournament.getJeu(), accent));
            dataGrid.addCell(valueCell(tournament.getDateDebut() != null ? tournament.getDateDebut().format(DATE_FMT) : "TBD", ColorConstants.WHITE));

            infoCell.add(dataGrid);
            mainTable.addCell(infoCell);

            // --- RIGHT SIDE: Stub with QR Code ---
            Cell stubCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBorderLeft(new DashedBorder(gray, 2))
                    .setPadding(20)
                    .setBackgroundColor(new DeviceRgb(26, 18, 58)) // Slightly lighter
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER);

            // Generate QR Code
            String qrData = "Tournament: " + tournament.getNom() + "\nGame: " + tournament.getJeu() + "\nDate: " + tournament.getDateDebut() + "\nPlayer: " + user.getNom();
            Image qrCodeImage = generateQRCode(qrData);
            qrCodeImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
            qrCodeImage.setWidth(100);
            
            stubCell.add(qrCodeImage);
            stubCell.add(new Paragraph("PRIZE POOL")
                    .setFontColor(gray)
                    .setFontSize(9)
                    .setMarginTop(15));
            stubCell.add(new Paragraph(String.format("%.0f €", tournament.getPrize()))
                    .setFontColor(new DeviceRgb(251, 191, 36)) // Gold
                    .setBold()
                    .setFontSize(16));

            mainTable.addCell(stubCell);

            document.add(mainTable);
            document.close();

            return new File(fileName).getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Cell labelCell(String text, DeviceRgb color) {
        return new Cell().add(new Paragraph(text).setFontSize(8).setFontColor(color)).setBorder(Border.NO_BORDER);
    }

    private Cell valueCell(String text, com.itextpdf.kernel.colors.Color color) {
        return new Cell().add(new Paragraph(text).setFontSize(12).setBold().setFontColor(color)).setBorder(Border.NO_BORDER);
    }

    private Image generateQRCode(String data) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 250, 250);
        
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        
        return new Image(ImageDataFactory.create(pngData));
    }
}
