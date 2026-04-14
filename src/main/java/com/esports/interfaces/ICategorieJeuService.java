package com.esports.interfaces;

import com.esports.model.CategorieJeu;
import java.util.List;

public interface ICategorieJeuService {
    boolean add(CategorieJeu c);
    boolean update(CategorieJeu c);
    boolean delete(int id);
    List<CategorieJeu> findAll();
    CategorieJeu findById(int id);
    boolean existsByName(String nom_categorie, int excludeId);
}
