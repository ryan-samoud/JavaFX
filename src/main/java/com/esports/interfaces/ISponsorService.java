package com.esports.interfaces;

import com.esports.model.Sponsor;

import java.util.List;

/**
 * INTERFACE — ISponsorService.java
 */
public interface ISponsorService {
    List<Sponsor> findAll();
    List<Sponsor> findByEvenement(int evenementId);
    boolean       save(Sponsor s);
    boolean       update(Sponsor s);
    boolean       delete(int id);
}
