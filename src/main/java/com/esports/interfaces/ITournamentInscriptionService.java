package com.esports.interfaces;

import com.esports.model.TournamentInscription;
import java.util.List;

public interface ITournamentInscriptionService {
    boolean register(TournamentInscription ti);
    boolean isRegistered(int tournoiId, int playerId);
    List<TournamentInscription> findByTournament(int tid);
    List<TournamentInscription> findByPlayer(int pid);
    boolean unregister(int id);
    boolean update(TournamentInscription ti);
}
