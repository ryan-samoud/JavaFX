package com.esports.interfaces;

import com.esports.model.Commentaire;
import java.util.List;

public interface ICommentaireService {
    List<Commentaire> findByEvenement(int evenementId);
    boolean save(Commentaire c);
    boolean update(Commentaire c);
    boolean delete(int id);
}
