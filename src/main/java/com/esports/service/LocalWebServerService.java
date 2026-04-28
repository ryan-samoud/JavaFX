package com.esports.service;

import com.esports.model.Jeu;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class LocalWebServerService {

    private static LocalWebServerService instance;

    public static synchronized LocalWebServerService getInstance() {
        if (instance == null) {
            instance = new LocalWebServerService();
        }
        return instance;
    }

    private HttpServer server;
    private int port = 8080;
    private String localIp = "127.0.0.1";
    private final JeuService jeuService = new JeuService();

    private LocalWebServerService() {
        // private constructor
    }

    public void startServer() {
        try {
            // Find a non-loopback IPv4 address
            localIp = getLocalIpAddress();
            
            // Try to start on port 8080, fallback to 8081 if in use
            try {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
                port = 8080;
            } catch (IOException e) {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8081), 0);
                port = 8081;
            }

            server.createContext("/jeu", new JeuHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("[LocalWebServer] Démarré sur http://" + localIp + ":" + port);
        } catch (Exception e) {
            System.err.println("[LocalWebServer] Erreur au démarrage du serveur local: " + e.getMessage());
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("[LocalWebServer] Serveur arrêté.");
        }
    }

    public String getServerBaseUrl() {
        return "http://" + localIp + ":" + port;
    }

    private String getLocalIpAddress() {
        String bestIp = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                
                // Ignore loopback, down interfaces, and common virtual adapters
                String name = ni.getDisplayName().toLowerCase();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual() 
                    || name.contains("virtual") || name.contains("vmware") 
                    || name.contains("vbox") || name.contains("virtualbox")
                    || name.contains("host-only") || name.contains("hyper-v")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    
                    // We only want IPv4
                    if (ip.contains(":")) continue;

                    // Prioritize standard local network ranges
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                        return ip; // This is almost certainly the physical WiFi/Ethernet IP
                    }
                    
                    bestIp = ip;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return bestIp;
    }

    private class JeuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            
            int id = -1;
            try {
                if (params.containsKey("id")) {
                    id = Integer.parseInt(params.get("id"));
                }
            } catch (Exception e) {
                // Ignore
            }

            Jeu jeu = jeuService.findById(id);
            String response;
            if (jeu == null) {
                response = "<h1>Jeu introuvable</h1>";
                exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
            } else {
                response = buildMobileHtml(jeu);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            }

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> map = new HashMap<>();
            if (query != null && !query.isBlank()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        map.put(pair.substring(0, idx), pair.substring(idx + 1));
                    }
                }
            }
            return map;
        }

        private String buildMobileHtml(Jeu jeu) {
            String nom = html(safe(jeu.getNom(), "Sans nom"));
            String mode = html(safe(jeu.getMode(), "Non défini"));
            String age = html(String.valueOf(jeu.getTrancheAge()));
            String players = html(String.valueOf(jeu.getNbJoueurs()));
            String note = html(String.valueOf(jeu.getNote()));
            String desc = html(safe(jeu.getDescription(), "Aucune description."));
            String cat = html(jeu.getCategorie() != null ? safe(jeu.getCategorie().getNomCategorie(), "N/A") : "N/A");
            
            String imageUrl = jeu.getImage();
            String heroStyle = "background: linear-gradient(135deg, #7c3aed, #ec4899); display: flex; align-items: center; justify-content: center;";
            String heroContent = "<span style='font-size: 60px;'>🎮</span>";
            
            if (imageUrl != null && !imageUrl.isBlank() && !imageUrl.equalsIgnoreCase("NULL")) {
                heroStyle = "background-image: url('" + html(imageUrl) + "'); background-size: cover; background-position: center;";
                heroContent = ""; // Empty if image exists
            }

            return "<!DOCTYPE html>"
                    + "<html lang='fr'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
                    + "<title>" + nom + " - NexUS</title>"
                    + "<style>"
                    + "  body { margin: 0; padding: 0; background-color: #090715; color: #e5e7eb; font-family: 'Segoe UI', Roboto, Helvetica, sans-serif; -webkit-font-smoothing: antialiased; }"
                    + "  .hero { width: 100%; height: 250px; " + heroStyle + " position: relative; border-bottom: 2px solid rgba(168,85,247,0.4); }"
                    + "  .hero-overlay { position: absolute; bottom: 0; left: 0; right: 0; padding: 30px 20px 20px; background: linear-gradient(to top, #090715 0%, transparent 100%); }"
                    + "  .title { margin: 0; font-size: 28px; font-weight: 800; color: #fff; text-shadow: 0 2px 10px rgba(0,0,0,0.8); }"
                    + "  .cat { margin: 5px 0 0; color: #c4b5fd; font-size: 14px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; }"
                    + "  .content { padding: 25px 20px; }"
                    + "  .desc { font-size: 15px; line-height: 1.6; color: #d1d5db; margin-bottom: 30px; }"
                    + "  .stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 30px; }"
                    + "  .stat-card { background: #14112b; border: 1px solid rgba(139,92,246,0.3); border-radius: 12px; padding: 15px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.2); }"
                    + "  .stat-label { font-size: 11px; text-transform: uppercase; color: #9ca3af; margin: 0 0 5px 0; }"
                    + "  .stat-value { font-size: 18px; font-weight: bold; margin: 0; }"
                    + "  .footer { text-align: center; padding: 20px; color: #4b5563; font-size: 12px; border-top: 1px solid rgba(255,255,255,0.05); margin-top: 20px; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "  <div class='hero'>"
                    +      heroContent
                    + "    <div class='hero-overlay'>"
                    + "      <h1 class='title'>" + nom + "</h1>"
                    + "      <p class='cat'>" + cat + "</p>"
                    + "    </div>"
                    + "  </div>"
                    + "  <div class='content'>"
                    + "    <p class='desc'>" + desc + "</p>"
                    + "    <div class='stats-grid'>"
                    + "      <div class='stat-card'>"
                    + "        <p class='stat-label'>Mode</p>"
                    + "        <p class='stat-value' style='color: #86efac;'>" + mode + "</p>"
                    + "      </div>"
                    + "      <div class='stat-card'>"
                    + "        <p class='stat-label'>Âge Requis</p>"
                    + "        <p class='stat-value' style='color: #93c5fd;'>" + age + "+</p>"
                    + "      </div>"
                    + "      <div class='stat-card'>"
                    + "        <p class='stat-label'>Joueurs</p>"
                    + "        <p class='stat-value' style='color: #fef08a;'>" + players + " 👤</p>"
                    + "      </div>"
                    + "      <div class='stat-card'>"
                    + "        <p class='stat-label'>Note</p>"
                    + "        <p class='stat-value' style='color: #fbbf24;'>" + note + " ⭐</p>"
                    + "      </div>"
                    + "    </div>"
                    + "  </div>"
                    + "  <div class='footer'>"
                    + "    © " + java.time.Year.now().getValue() + " NexUS eSports Mobile View"
                    + "  </div>"
                    + "</body>"
                    + "</html>";
        }
        
        private String html(String value) {
            return value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }
        
        private String safe(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }
}
