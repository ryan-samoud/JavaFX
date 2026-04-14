package com.esports.interfaces;

import com.esports.model.User;

import java.util.List;
import java.util.Optional;

/**
 * INTERFACE — IUserService.java
 * Contrat de la couche service pour les utilisateurs.
 * Permet de découpler les controllers de l'implémentation concrète.
 */
public interface IUserService {

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findRecent(int limit);

    int countActive();

    boolean deactivate(int id);
}
