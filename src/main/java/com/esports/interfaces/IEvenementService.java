package com.esports.interfaces;

import com.esports.model.Evenement;
import java.util.List;

public interface IEvenementService {
    List<Evenement> findAll();
    Evenement       findById(int id);
    int             countAll();
    boolean         save(Evenement e);
    boolean         update(Evenement e);
    boolean         delete(int id);
    void            incrementParticipants(int id);
    void            decrementParticipants(int id);
    boolean         existsByNom(String nom, int excludeId); // for unique name check
}