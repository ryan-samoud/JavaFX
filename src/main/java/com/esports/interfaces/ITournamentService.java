package com.esports.interfaces;

import com.esports.model.Tournament;

import java.util.List;

/**
 * INTERFACE — ITournamentService.java
 * Contrat de la couche service pour les tournois.
 * Permet de découpler les controllers de l'implémentation concrète.
 */
public interface ITournamentService {

    List<Tournament> findAll();

    int countAll();

    boolean save(Tournament tournament);

    boolean update(Tournament tournament);

    boolean delete(int id);
}
