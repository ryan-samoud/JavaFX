package com.esports.service;

import com.esports.model.Tournament;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVICE — GoogleCalendarService.java
 * Génère les URLs Google Calendar pour ajouter les tournois
 * et construit le contenu HTML du calendrier embarqué.
 */
public class GoogleCalendarService {

    private static final DateTimeFormatter GCAL_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final String GCAL_EVENT_BASE = "https://calendar.google.com/calendar/event?action=TEMPLATE";

    /**
     * Génère l'URL Google Calendar pour ajouter un tournoi comme événement.
     */
    public String buildAddEventUrl(Tournament t) {
        StringBuilder url = new StringBuilder(GCAL_EVENT_BASE);

        // Titre
        url.append("&text=").append(encode("🏆 " + t.getNom() + " — " + t.getJeu()));

        // Dates
        if (t.getDateDebut() != null) {
            String start = t.getDateDebut().format(GCAL_FMT);
            String end;
            if (t.getDateFin() != null) {
                end = t.getDateFin().format(GCAL_FMT);
            } else {
                // Par défaut, durée de 3 heures
                end = t.getDateDebut().plusHours(3).format(GCAL_FMT);
            }
            url.append("&dates=").append(start).append("/").append(end);
        }

        // Description
        String details = "🎮 Jeu: " + t.getJeu() + "\n" +
                          "💰 Prize Pool: " + String.format("%.0f€", t.getPrize()) + "\n" +
                          "👥 Participants: " + (t.getNbParticipantsActuels() != null ? t.getNbParticipantsActuels() : 0) 
                              + "/" + t.getNbParticipantsMax() + "\n" +
                          "📋 Statut: " + t.getStatut() + "\n\n" +
                          "Organisé par NexUS Gaming Arena";
        url.append("&details=").append(encode(details));

        // Lieu
        String loc = (t.getLocation() != null && !t.getLocation().isEmpty())
                     ? t.getLocation()
                     : "NexUS Gaming Arena — Online";
        url.append("&location=").append(encode(loc));

        return url.toString();
    }

    /**
     * Construit le HTML interactif du calendrier embarqué avec tous les tournois.
     * Utilise FullCalendar (CDN) pour un rendu premium avec le thème NexUS.
     */
    public String buildCalendarHtml(List<Tournament> tournaments, int year, int month) {
        // Construire les événements JSON
        String eventsJson = tournaments.stream()
                .filter(t -> t.getDateDebut() != null)
                .map(t -> {
                    String color = switch (t.getStatut()) {
                        case "OUVERT" -> "#4ade80";
                        case "DRAFT" -> "#00b8ff";
                        case "EN_COURS" -> "#ec4899";
                        case "TERMINE" -> "#4b5563";
                        default -> "#a855f7";
                    };
                    String end = t.getDateFin() != null
                            ? t.getDateFin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : t.getDateDebut().plusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    String gcalUrl = buildAddEventUrl(t).replace("'", "\\'").replace("\"", "\\\"");

                    return String.format(
                        "{title:'%s',start:'%s',end:'%s',color:'%s',extendedProps:{jeu:'%s',prize:'%.0f€',statut:'%s',participants:'%s/%d',gcalUrl:'%s'}}",
                        escapeJs(t.getNom()),
                        t.getDateDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        end,
                        color,
                        escapeJs(t.getJeu()),
                        t.getPrize(),
                        t.getStatut(),
                        (t.getNbParticipantsActuels() != null ? t.getNbParticipantsActuels() : 0),
                        t.getNbParticipantsMax(),
                        gcalUrl
                    );
                })
                .collect(Collectors.joining(","));

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <link href="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.11/index.global.min.css" rel="stylesheet">
            <script src="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.11/index.global.min.js"></script>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                    background: #0d0b1e;
                    color: #e2d9f3;
                    font-family: 'Segoe UI', Arial, sans-serif;
                    padding: 20px;
                    overflow-x: hidden;
                }

                /* ── FullCalendar Theme Override ────────────────────── */
                .fc {
                    --fc-border-color: rgba(139, 92, 246, 0.2);
                    --fc-button-bg-color: rgba(168, 85, 247, 0.15);
                    --fc-button-border-color: #a855f7;
                    --fc-button-text-color: #c4b5fd;
                    --fc-button-active-bg-color: #a855f7;
                    --fc-button-active-border-color: #a855f7;
                    --fc-button-hover-bg-color: rgba(168, 85, 247, 0.3);
                    --fc-button-hover-border-color: #a855f7;
                    --fc-today-bg-color: rgba(168, 85, 247, 0.08);
                    --fc-page-bg-color: #0d0b1e;
                    --fc-neutral-bg-color: #110f28;
                    --fc-event-border-color: transparent;
                    --fc-now-indicator-color: #ec4899;
                }

                .fc .fc-toolbar-title {
                    color: white !important;
                    font-size: 1.5em !important;
                    font-weight: 800 !important;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                }

                .fc .fc-col-header-cell-cushion {
                    color: #a855f7 !important;
                    font-weight: 700;
                    text-transform: uppercase;
                    font-size: 11px;
                    letter-spacing: 1px;
                }

                .fc .fc-daygrid-day-number {
                    color: #c4b5fd !important;
                    font-weight: 500;
                }

                .fc .fc-day-today .fc-daygrid-day-number {
                    background: #a855f7;
                    color: white !important;
                    border-radius: 50%;
                    width: 28px; height: 28px;
                    display: flex; align-items: center; justify-content: center;
                }

                .fc-event {
                    border-radius: 6px !important;
                    padding: 3px 6px !important;
                    font-size: 11px !important;
                    font-weight: 600 !important;
                    cursor: pointer !important;
                    border: none !important;
                    transition: transform 0.15s ease, box-shadow 0.15s ease;
                }
                .fc-event:hover {
                    transform: scale(1.03);
                    box-shadow: 0 4px 15px rgba(168, 85, 247, 0.4);
                }

                .fc .fc-button {
                    border-radius: 8px !important;
                    font-weight: 600 !important;
                    text-transform: uppercase !important;
                    font-size: 11px !important;
                    letter-spacing: 0.5px;
                    transition: all 0.2s ease;
                }

                .fc .fc-scrollgrid { border-radius: 12px; overflow: hidden; }
                .fc th { background: rgba(168, 85, 247, 0.05); }

                /* ── Tooltip / Popup ──────────────────────────────── */
                #popup-overlay {
                    display: none;
                    position: fixed; top: 0; left: 0; width: 100%; height: 100%;
                    background: rgba(0,0,0,0.6); z-index: 999;
                    justify-content: center; align-items: center;
                }
                #popup-overlay.active { display: flex; }

                .popup-card {
                    background: #110f28;
                    border: 2px solid #a855f7;
                    border-radius: 16px;
                    padding: 30px;
                    min-width: 340px;
                    max-width: 420px;
                    box-shadow: 0 20px 60px rgba(168, 85, 247, 0.3);
                    animation: popIn 0.25s ease;
                }
                @keyframes popIn {
                    from { transform: scale(0.85); opacity: 0; }
                    to { transform: scale(1); opacity: 1; }
                }

                .popup-card h2 {
                    color: white; font-size: 18px;
                    margin-bottom: 16px; font-weight: 800;
                }
                .popup-card .info-row {
                    display: flex; justify-content: space-between;
                    padding: 8px 0;
                    border-bottom: 1px solid rgba(139, 92, 246, 0.1);
                    font-size: 13px;
                }
                .popup-card .info-label { color: #7c6fa8; }
                .popup-card .info-value { color: #e2d9f3; font-weight: 600; }
                .popup-card .info-value.prize { color: #fbbf24; }

                .btn-gcal {
                    display: inline-flex; align-items: center; gap: 8px;
                    margin-top: 20px; padding: 12px 24px;
                    background: linear-gradient(135deg, #4285f4, #34a853);
                    color: white; border: none; border-radius: 10px;
                    font-weight: 700; font-size: 13px;
                    cursor: pointer; width: 100%;
                    justify-content: center;
                    transition: transform 0.15s ease, box-shadow 0.2s ease;
                }
                .btn-gcal:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 8px 25px rgba(66, 133, 244, 0.4);
                }
                .btn-gcal img {
                    width: 18px; height: 18px;
                }

                .btn-close-popup {
                    margin-top: 10px; padding: 8px 20px;
                    background: transparent;
                    color: #9ca3af; border: 1px solid #4b5563;
                    border-radius: 8px; cursor: pointer;
                    font-size: 12px; width: 100%;
                    transition: border-color 0.2s;
                }
                .btn-close-popup:hover { border-color: #a855f7; color: #c4b5fd; }

                /* ── Header Badge ─────────────────────────────────── */
                .header-bar {
                    display: flex; align-items: center; gap: 12px;
                    margin-bottom: 20px;
                }
                .header-bar .logo {
                    background: #a855f7;
                    color: white; font-weight: 800;
                    padding: 6px 12px; border-radius: 8px;
                    font-size: 14px;
                    box-shadow: 0 0 15px rgba(168, 85, 247, 0.5);
                }
                .header-bar .title {
                    font-size: 20px; font-weight: 800;
                    background: linear-gradient(135deg, #a855f7, #ec4899);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    letter-spacing: 1px;
                }
                .header-bar .g-icon {
                    margin-left: auto;
                    display: flex; align-items: center; gap: 6px;
                    color: #9ca3af; font-size: 12px;
                }
                .g-badge {
                    display: inline-flex; align-items: center; gap: 4px;
                    background: rgba(66, 133, 244, 0.1);
                    border: 1px solid rgba(66, 133, 244, 0.3);
                    padding: 4px 10px; border-radius: 20px;
                    font-size: 11px; color: #93c5fd; font-weight: 600;
                }
            </style>
        </head>
        <body>
            <div class="header-bar">
                <span class="logo">N</span>
                <span class="title">CALENDRIER DES TOURNOIS</span>
                <div class="g-icon">
                    <span class="g-badge">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                            <path d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12c0 5.523 4.477 10 10 10s10-4.477 10-10z" fill="#4285F4" opacity="0.2"/>
                            <path d="M17 12h-4V8h-2v4H7v2h4v4h2v-4h4v-2z" fill="#4285F4"/>
                        </svg>
                        Google Calendar
                    </span>
                </div>
            </div>

            <div id="calendar"></div>

            <div id="popup-overlay" onclick="if(event.target===this)closePopup()">
                <div class="popup-card" id="popup-content"></div>
            </div>

            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    var events = [{{EVENTS_JSON}}];

                    var calendar = new FullCalendar.Calendar(document.getElementById('calendar'), {
                        initialView: 'dayGridMonth',
                        initialDate: '{{INITIAL_DATE}}',
                        locale: 'fr',
                        headerToolbar: {
                            left: 'prev,next today',
                            center: 'title',
                            right: 'dayGridMonth,timeGridWeek,listWeek'
                        },
                        buttonText: {
                            today: "Aujourd'hui",
                            month: 'Mois',
                            week: 'Semaine',
                            list: 'Liste'
                        },
                        height: 'auto',
                        events: events,
                        eventClick: function(info) {
                            info.jsEvent.preventDefault();
                            var e = info.event;
                            var p = e.extendedProps;
                            var startStr = e.start ? e.start.toLocaleDateString('fr-FR', {day:'2-digit',month:'long',year:'numeric',hour:'2-digit',minute:'2-digit'}) : 'TBD';
                            var endStr = e.end ? e.end.toLocaleDateString('fr-FR', {day:'2-digit',month:'long',year:'numeric',hour:'2-digit',minute:'2-digit'}) : '';

                            document.getElementById('popup-content').innerHTML =
                                '<h2>🏆 ' + e.title + '</h2>' +
                                '<div class="info-row"><span class="info-label">🎮 Jeu</span><span class="info-value">' + p.jeu + '</span></div>' +
                                '<div class="info-row"><span class="info-label">💰 Prize Pool</span><span class="info-value prize">' + p.prize + '</span></div>' +
                                '<div class="info-row"><span class="info-label">📋 Statut</span><span class="info-value">' + p.statut + '</span></div>' +
                                '<div class="info-row"><span class="info-label">👥 Participants</span><span class="info-value">' + p.participants + '</span></div>' +
                                '<div class="info-row"><span class="info-label">📅 Début</span><span class="info-value">' + startStr + '</span></div>' +
                                (endStr ? '<div class="info-row"><span class="info-label">🏁 Fin</span><span class="info-value">' + endStr + '</span></div>' : '') +
                                '<button class="btn-gcal" onclick="addToGCal(\\'' + p.gcalUrl.replace(/'/g, "\\\\'") + '\\')">' +
                                    '<svg width="18" height="18" viewBox="0 0 24 24"><rect x="2" y="3" width="20" height="19" rx="3" fill="#fff" opacity="0.2"/><path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10z" fill="white"/></svg>' +
                                    'Ajouter à Google Calendar' +
                                '</button>' +
                                '<button class="btn-close-popup" onclick="closePopup()">Fermer</button>';

                            document.getElementById('popup-overlay').classList.add('active');
                        },
                        eventDidMount: function(info) {
                            info.el.title = info.event.title + ' — ' + info.event.extendedProps.jeu;
                        }
                    });
                    calendar.render();
                });

                function addToGCal(url) {
                    // JavaFX WebView: use window.location or try java bridge
                    try {
                        if (window.javaBridge) {
                            window.javaBridge.openUrl(url);
                        } else {
                            window.location.href = url;
                        }
                    } catch(e) {
                        window.location.href = url;
                    }
                }

                function closePopup() {
                    document.getElementById('popup-overlay').classList.remove('active');
                }
            </script>
        </body>
        </html>
        """.replace("{{EVENTS_JSON}}", eventsJson)
           .replace("{{INITIAL_DATE}}", String.format("%d-%02d-01", year, month));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
