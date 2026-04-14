package com.esports.interfaces;

import com.esports.model.Jeu;
import java.util.List;

public interface IJeuService {
    boolean add(Jeu j);
    boolean update(Jeu j);
    boolean delete(int id);
    List<Jeu> findAll();
    Jeu findById(int id);
    boolean existsByName(String nom, int excludeId);
}
