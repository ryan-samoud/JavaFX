package com.esports.interfaces;

import com.esports.model.Evenement;

import java.util.List;

/**
 * INTERFACE — IEvenementService.java
 */
public interface IEvenementService {
    List<Evenement> findAll();
    Evenement       findById(int id);
    int             countAll();
    boolean         save(Evenement e);
    boolean         update(Evenement e);
    boolean         delete(int id);
}
