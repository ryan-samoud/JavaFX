package com.esports.interfaces;

import com.esports.model.Tournament;
import java.util.List;

public interface ITournamentService {
    List<Tournament> findAll();
    int countAll();
    boolean save(Tournament tournament);
    boolean update(Tournament tournament);
    boolean delete(int id);
    Tournament findById(int id);
    boolean existsByName(String name);
    boolean existsByNameExcludeId(String name, int id);
}
