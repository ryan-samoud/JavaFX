package com.esports.utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * EmailService — sends password-reset codes via Gmail SMTP.
 *
 * SETUP (required before use):
 *  1. Enable 2-Step Verification on your Google account.
 *  2. Go to: Google Account → Security → 2-Step Verification → App passwords.
 *  3. Create an app password for "Mail / Windows Computer".
 *  4. Replace SENDER_EMAIL and SENDER_PASS below with your Gmail address
 *     and the 16-character app password (no spaces).
 */
public class EmailService {

    // ── Configure these two values ──────────────────────────────────
    private static final String SENDER_EMAIL = "liwa.antar2@gmail.com";   // <- your Gmail
    private static final String SENDER_PASS  = "ywvj hnlz pjmh rkbh";    // <- App Password
    // ────────────────────────────────────────────────────────────────

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final int    SMTP_PORT  = 587;
    private static final String APP_NAME   = "NexUS Esports";

    public static void sendPasswordResetCode(String toEmail,
                                             String userName,
                                             String code)
            throws MessagingException, java.io.UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SENDER_EMAIL, APP_NAME));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("NexUS — Réinitialisation de mot de passe");
        msg.setContent(buildHtml(userName, code), "text/html; charset=UTF-8");

        Transport.send(msg);
        System.out.println("[EmailService] Reset code sent to " + toEmail);
    }

    private static String buildHtml(String userName, String code) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
             + "<body style='margin:0;padding:0;background:#0d0b1e;font-family:Arial,sans-serif;'>"
             + "<table width='100%' cellpadding='0' cellspacing='0'>"
             + "<tr><td align='center' style='padding:40px 20px;'>"
             + "<table width='500' cellpadding='0' cellspacing='0' style='"
             +   "background:#1a1535;border-radius:12px;"
             +   "border:1px solid rgba(168,85,247,0.35);"
             +   "box-shadow:0 0 40px rgba(168,85,247,0.2);'>"
             // Header
             + "<tr><td style='"
             +   "background:linear-gradient(to right,#4c1d95,#7c3aed);"
             +   "padding:24px 36px;border-radius:12px 12px 0 0;'>"
             + "<h1 style='color:white;margin:0;font-size:20px;letter-spacing:3px;'>"
             +   "&#9670; NexUS Esports</h1>"
             + "</td></tr>"
             // Body
             + "<tr><td style='padding:36px;'>"
             + "<h2 style='color:#e2d9f3;margin:0 0 16px;font-size:18px;'>Réinitialisation de mot de passe</h2>"
             + "<p style='color:#a78bba;font-size:15px;line-height:1.6;margin:0 0 10px;'>"
             +   "Bonjour <strong style='color:#c4b5fd;'>" + escapeHtml(userName) + "</strong>,</p>"
             + "<p style='color:#a78bba;font-size:15px;line-height:1.6;margin:0 0 24px;'>"
             +   "Voici votre code de vérification :</p>"
             // Code block
             + "<div style='text-align:center;margin:0 0 28px;'>"
             + "<div style='"
             +   "display:inline-block;"
             +   "background:rgba(168,85,247,0.12);"
             +   "border:2px solid rgba(168,85,247,0.55);"
             +   "border-radius:12px;padding:20px 48px;'>"
             + "<span style='"
             +   "font-size:40px;font-weight:bold;color:#a855f7;"
             +   "letter-spacing:10px;font-family:\"Courier New\",monospace;'>"
             + code + "</span>"
             + "</div></div>"
             + "<p style='color:#a78bba;font-size:14px;margin:0 0 8px;'>"
             +   "Ce code est valable pendant <strong style='color:#c4b5fd;'>15 minutes</strong>.</p>"
             + "<p style='color:#a78bba;font-size:14px;margin:0;'>"
             +   "Si vous n'avez pas demandé cette réinitialisation, ignorez cet e-mail.</p>"
             + "<hr style='border:none;border-top:1px solid rgba(168,85,247,0.2);margin:28px 0;'/>"
             + "<p style='color:#6b5a7e;font-size:12px;text-align:center;margin:0;'>"
             +   "NexUS Esports — Plateforme de tournois</p>"
             + "</td></tr></table>"
             + "</td></tr></table></body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}