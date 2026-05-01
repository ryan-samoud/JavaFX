package com.esports.service;

import com.esports.interfaces.ITournamentService;
import com.esports.interfaces.ITournamentInscriptionService;
import com.esports.model.Tournament;
import com.esports.model.TournamentInscription;
import com.esports.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class LocalChatbotService {

    private final ITournamentService tournamentService = new TournamentService();
    private final ITournamentInscriptionService inscriptionService = new TournamentInscriptionService();

    public String getResponse(String message, User currentUser) {
        String msg = message.toLowerCase().trim();

        if (msg.contains("aide") || msg.contains("help") || msg.contains("quoi")) {
            return "🤖 Voici ce que je peux faire :\n" +
                   "- Lister les 'tournois'\n" +
                   "- Voir mes 'inscriptions'\n" +
                   "- Infos sur un jeu (ex: 'LoL', 'Valorant')\n" +
                   "- Voir le 'prize' pool global";
        }

        if (msg.contains("bonjour") || msg.contains("salut") || msg.contains("coucou") || msg.equals("hi") || msg.equals("hello")) {
            return "🤖 Bonjour ! Je suis l'assistant NexUS. Je peux vous renseigner sur les tournois et les inscriptions locaux. Tapez 'aide' pour voir mes capacités !";
        }

        if (msg.contains("tournoi")) {
            List<Tournament> list = tournamentService.findAll();
            if (list.isEmpty()) return "🤖 Aucun tournoi n'est programmé pour le moment.";
            
            String tournois = list.stream()
                .limit(5)
                .map(t -> "• " + t.getNom() + " (" + t.getJeu() + ") - " + t.getStatut())
                .collect(Collectors.joining("\n"));
            return "🤖 Voici les tournois actuels (top 5) :\n" + tournois + (list.size() > 5 ? "\n... et d'autres encore !" : "");
        }

        if (msg.contains("inscrit") || msg.contains("mes inscriptions")) {
            if (currentUser == null) return "🤖 Vous n'êtes pas connecté. Connectez-vous pour voir vos inscriptions.";
            
            List<TournamentInscription> myInsc = inscriptionService.findByPlayer(currentUser.getId());
            if (myInsc.isEmpty()) return "🤖 Vous n'avez aucune inscription active.";
            
            return "🤖 Vous êtes inscrit à " + myInsc.size() + " tournoi(s).";
        }

        if (msg.contains("prize") || msg.contains("cash")) {
            double total = tournamentService.findAll().stream().mapToDouble(Tournament::getPrize).sum();
            return "🤖 Le cashprize total cumulé sur NexUS est de " + String.format("%.0f", total) + "€ ! 💰";
        }

        if (msg.contains("lol") || msg.contains("league")) {
            return "🤖 League of Legends est très populaire ici ! Nous avons plusieurs tournois prévus. Consultez la liste pour ne rien rater.";
        }

        if (msg.contains("valorant")) {
            return "🤖 Valorant ! Le FPS tactique de Riot. Plusieurs équipes s'affrontent ce mois-ci.";
        }
        
        if (msg.contains("score") || msg.contains("résultat")) {
            return "🤖 Les scores des matchs sont disponibles dans la section 'Matchs' (en cours de développement).";
        }

        return null; // Laisser l'IA répondre si aucun mot-clé local n'est trouvé
    }
}
