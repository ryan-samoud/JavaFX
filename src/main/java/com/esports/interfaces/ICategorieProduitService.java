package com.esports.interfaces;

import com.esports.model.CategorieProduit;

import java.util.List;
import java.util.Optional;

/**
 * INTERFACE — ICategorieProduitService.java
 * Définit les opérations CRUD pour les catégories de produits
 */
public interface ICategorieProduitService {

    /**
     * Créer une nouvelle catégorie
     * @param categorie La catégorie à créer
     * @return true si succès, false sinon
     */
    boolean create(CategorieProduit categorie);

    /**
     * Récupérer toutes les catégories
     * @return Liste des catégories
     */
    List<CategorieProduit> findAll();

    /**
     * Trouver une catégorie par son ID
     * @param id L'identifiant de la catégorie
     * @return Optional contenant la catégorie si trouvée
     */
    Optional<CategorieProduit> findById(int id);

    /**
     * Mettre à jour une catégorie existante
     * @param categorie La catégorie avec les nouvelles données
     * @return true si succès, false sinon
     */
    boolean update(CategorieProduit categorie);

    /**
     * Supprimer une catégorie
     * @param id L'identifiant de la catégorie à supprimer
     * @return true si succès, false sinon
     */
    boolean delete(int id);

    /**
     * Compter le nombre de catégories
     * @return Le nombre total de catégories
     */
    int count();

    /**
     * Rechercher des catégories par nom
     * @param searchTerm Le terme de recherche
     * @return Liste des catégories correspondantes
     */
    List<CategorieProduit> search(String searchTerm);
}