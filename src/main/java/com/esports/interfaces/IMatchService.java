package com.esports.interfaces;

import com.esports.model.Match;
import java.util.List;

public interface IMatchService {
    List<Match> findAll();
    boolean save(Match m);
    boolean update(Match m);
    boolean delete(int id);
    Match findById(int id);
    List<Match> findByTournamentId(int tid);
}
