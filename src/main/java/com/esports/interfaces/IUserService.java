package com.esports.interfaces;

import com.esports.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * INTERFACE — IUserService.java
 * Contrat de la couche service pour les utilisateurs.
 */
public interface IUserService {

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findAllUsers();

    List<User> findRecent(int limit);

    int countActive();

    boolean save(User user);

    boolean update(User user);

    boolean deactivate(int id);

    boolean ban(int id, String reason);

    boolean unban(int id);

    boolean suspend(int id, LocalDateTime until, String reason);
}
