package com.esports.service;

import com.esports.model.Jeu;
import com.esports.model.User;
import com.esports.utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Sends notification emails when a new Jeu is created.
 */
public class JeuMailService {

    private final String smtpHost = readEnv("NEXUS_SMTP_HOST", "smtp.gmail.com");
    private final String smtpPort = readEnv("NEXUS_SMTP_PORT", "587");
    private final String smtpUsername = readEnv("NEXUS_SMTP_USERNAME", "gharbimoemen480@gmail.com");
    private final String smtpPassword = readEnv("NEXUS_SMTP_PASSWORD", "ughqjehplsjxjbec");
    private final String fromEmail = readEnv("NEXUS_MAIL_FROM", smtpUsername);
    private final String notifyEmail = readEnv("NEXUS_NOTIFY_EMAIL", "gharbimoemen480@gmail.com");
    private final UserService userService = new UserService();

    public void sendJeuCreatedEmail(Jeu jeu) {
        if (jeu == null || smtpUsername.isBlank() || smtpPassword.isBlank()) {
            return;
        }
        List<String> playerEmails = resolvePlayerEmails();
        if (playerEmails.isEmpty()) {
            System.out.println("[JeuMailService] Aucun player actif trouve, email non envoye.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            // Keep sender/admin in TO to satisfy SMTP providers, send players via BCC.
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(resolveConnectedAdminEmail()));
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(String.join(",", playerEmails)));
            message.setSubject("Nouveau jeu ajoute: " + safe(jeu.getNom(), "Sans nom"));
            message.setContent(buildHtmlBody(jeu), "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("[JeuMailService] Email envoye aux players pour le nouveau jeu: " + safe(jeu.getNom(), "Sans nom"));
        } catch (MessagingException e) {
            System.err.println("[JeuMailService] Erreur envoi mail: " + e.getMessage());
        }
    }

    private String buildHtmlBody(Jeu jeu) {
        String nom = html(safe(jeu.getNom(), "Sans nom"));
        String mode = html(safe(jeu.getMode(), "Non défini"));
        String age = html(String.valueOf(jeu.getTrancheAge()));
        String players = html(String.valueOf(jeu.getNbJoueurs()));
        String note = html(String.valueOf(jeu.getNote()));
        String desc = html(safe(jeu.getDescription(), "Aucune description fournie pour ce jeu."));
        String cat = html(jeu.getCategorie() != null ? safe(jeu.getCategorie().getNomCategorie(), "N/A") : "N/A");
        
        String heroImage = "";
        String imageUrl = jeu.getImage();
        if (imageUrl != null && !imageUrl.isBlank() && !imageUrl.equalsIgnoreCase("NULL")) {
            // Include image if it's a valid URL or Cloudinary path
            heroImage = "<div style='width:100%;height:240px;background-color:#1a1635;border-bottom:1px solid rgba(168,85,247,0.3);overflow:hidden;'>"
                      + "<img src='" + html(imageUrl) + "' alt='Cover' style='width:100%;height:100%;object-fit:cover;object-position:center;'/>"
                      + "</div>";
        } else {
            // Placeholder hero
            heroImage = "<div style='width:100%;height:140px;background:linear-gradient(to bottom, #1a1635, #0d0b1e);border-bottom:1px solid rgba(168,85,247,0.3);display:table;text-align:center;'>"
                      + "<div style='display:table-cell;vertical-align:middle;font-size:48px;'>🎮</div>"
                      + "</div>";
        }

        return "<!DOCTYPE html>"
                + "<html lang='fr'>"
                + "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background-color:#090715;font-family:\"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;-webkit-font-smoothing:antialiased;'>"
                + "  <table width='100%' cellpadding='0' cellspacing='0' border='0' style='background-color:#090715;padding:30px 10px;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <!-- Main Container -->"
                + "        <table width='100%' max-width='640' cellpadding='0' cellspacing='0' border='0' style='max-width:640px;background-color:#0d0b1e;border-radius:16px;border:1px solid rgba(139,92,246,0.3);overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.5);'>"
                + "          "
                + "          <!-- Header / Gradient Bar -->"
                + "          <tr>"
                + "            <td style='background:linear-gradient(135deg, #7c3aed, #ec4899);padding:24px 30px;text-align:center;'>"
                + "              <h1 style='margin:0;color:#ffffff;font-size:24px;font-weight:800;letter-spacing:1px;text-transform:uppercase;'>✨ Nouveauté NexUS</h1>"
                + "            </td>"
                + "          </tr>"
                + "          "
                + "          <!-- Hero Image -->"
                + "          <tr>"
                + "            <td>"
                + heroImage
                + "            </td>"
                + "          </tr>"
                + "          "
                + "          <!-- Content Body -->"
                + "          <tr>"
                + "            <td style='padding:35px 40px;'>"
                + "              <h2 style='margin:0 0 10px 0;color:#ffffff;font-size:28px;font-weight:800;'>" + nom + "</h2>"
                + "              <p style='margin:0 0 25px 0;color:#a78bfa;font-size:16px;font-weight:600;text-transform:uppercase;letter-spacing:1px;'>" + cat + "</p>"
                + "              "
                + "              <p style='margin:0 0 30px 0;color:#d1d5db;font-size:15px;line-height:1.6;'>" + desc + "</p>"
                + "              "
                + "              <!-- Stats Grid -->"
                + "              <table width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-bottom:35px;'>"
                + "                <tr>"
                + "                  <!-- Stat: Mode -->"
                + "                  <td width='48%' style='background-color:#14112b;border:1px solid rgba(168,85,247,0.2);border-radius:10px;padding:15px;text-align:center;'>"
                + "                    <p style='margin:0 0 5px 0;color:#9ca3af;font-size:12px;text-transform:uppercase;'>Mode de jeu</p>"
                + "                    <p style='margin:0;color:#86efac;font-size:16px;font-weight:bold;'>" + mode + "</p>"
                + "                  </td>"
                + "                  <td width='4%'></td>"
                + "                  <!-- Stat: Age -->"
                + "                  <td width='48%' style='background-color:#14112b;border:1px solid rgba(168,85,247,0.2);border-radius:10px;padding:15px;text-align:center;'>"
                + "                    <p style='margin:0 0 5px 0;color:#9ca3af;font-size:12px;text-transform:uppercase;'>Âge requis</p>"
                + "                    <p style='margin:0;color:#93c5fd;font-size:16px;font-weight:bold;'>" + age + "+ ans</p>"
                + "                  </td>"
                + "                </tr>"
                + "                <tr><td colspan='3' height='15'></td></tr>"
                + "                <tr>"
                + "                  <!-- Stat: Players -->"
                + "                  <td width='48%' style='background-color:#14112b;border:1px solid rgba(168,85,247,0.2);border-radius:10px;padding:15px;text-align:center;'>"
                + "                    <p style='margin:0 0 5px 0;color:#9ca3af;font-size:12px;text-transform:uppercase;'>Joueurs Max</p>"
                + "                    <p style='margin:0;color:#fef08a;font-size:16px;font-weight:bold;'>" + players + " 👤</p>"
                + "                  </td>"
                + "                  <td width='4%'></td>"
                + "                  <!-- Stat: Note -->"
                + "                  <td width='48%' style='background-color:#14112b;border:1px solid rgba(168,85,247,0.2);border-radius:10px;padding:15px;text-align:center;'>"
                + "                    <p style='margin:0 0 5px 0;color:#9ca3af;font-size:12px;text-transform:uppercase;'>Note initiale</p>"
                + "                    <p style='margin:0;color:#fbbf24;font-size:16px;font-weight:bold;'>" + note + " / 5 ⭐</p>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "              "
                + "              <!-- Call to Action -->"
                + "              <div style='text-align:center;'>"
                + "                <a href='#' style='display:inline-block;background:linear-gradient(to right, #7c3aed, #ec4899);color:#ffffff;text-decoration:none;font-size:16px;font-weight:bold;padding:14px 35px;border-radius:30px;text-transform:uppercase;letter-spacing:1px;box-shadow:0 4px 15px rgba(236,72,153,0.4);'>Découvrir le jeu</a>"
                + "              </div>"
                + "              "
                + "            </td>"
                + "          </tr>"
                + "          "
                + "          <!-- Footer -->"
                + "          <tr>"
                + "            <td style='background-color:#090715;padding:25px;text-align:center;border-top:1px solid rgba(255,255,255,0.05);'>"
                + "              <p style='margin:0 0 10px 0;color:#6b7280;font-size:12px;'>"
                + "                Cet email a été envoyé automatiquement aux joueurs actifs de la plateforme <strong>NexUS</strong>."
                + "              </p>"
                + "              <p style='margin:0;color:#4b5563;font-size:11px;'>"
                + "                © " + java.time.Year.now().getValue() + " NexUS eSports. Tous droits réservés."
                + "              </p>"
                + "            </td>"
                + "          </tr>"
                + "          "
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private List<String> resolvePlayerEmails() {
        List<String> emails = new ArrayList<>();

        // Requete directe SQL — contourne UserService pour fiabilite maximale
        String sql = "SELECT email, role FROM user WHERE is_active = 1 AND email IS NOT NULL AND email != ''";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String email = rs.getString("email");
                    String role  = rs.getString("role");
                    System.out.println("[JeuMailService] DB row -> email=" + email + ", role=[" + role + "]");

                    if (role != null && role.toLowerCase().contains("player")) {
                        emails.add(email.trim());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[JeuMailService] SQL error resolvePlayerEmails: " + e.getMessage());
        }

        // Fallback: si aucun player trouve, envoyer a tous les utilisateurs actifs
        if (emails.isEmpty()) {
            System.out.println("[JeuMailService] Aucun player par role, fallback: envoi a tous les utilisateurs actifs.");
            String fallbackSql = "SELECT email FROM user WHERE is_active = 1 AND email IS NOT NULL AND email != ''";
            try {
                Connection conn = DatabaseConnection.getInstance();
                try (PreparedStatement stmt = conn.prepareStatement(fallbackSql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String email = rs.getString("email");
                        if (email != null && !email.isBlank()) {
                            emails.add(email.trim());
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("[JeuMailService] SQL error fallback: " + e.getMessage());
            }
        }

        System.out.println("[JeuMailService] Emails finaux a notifier: " + emails);
        return emails.stream().distinct().collect(Collectors.toList());
    }

    private String resolveConnectedAdminEmail() {
        User current = AuthService.getCurrentUser();
        if (current != null && current.isAdmin() && current.getEmail() != null && !current.getEmail().isBlank()) {
            return current.getEmail().trim();
        }
        return notifyEmail != null ? notifyEmail.trim() : "";
    }

    private String html(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
