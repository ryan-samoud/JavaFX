package com.esports.interfaces;

import com.esports.model.Produit;

import java.util.List;
import java.util.Optional;

/**
 * INTERFACE — IProduitService.java
 * Définit les opérations CRUD pour les produits
 */
public interface IProduitService {

    /**
     * Créer un nouveau produit
     * @param produit Le produit à créer
     * @return true si succès, false sinon
     */
    boolean create(Produit produit);

    /**
     * Récupérer tous les produits
     * @return Liste des produits
     */
    List<Produit> findAll();

    /**
     * Trouver un produit par son ID
     * @param id L'identifiant du produit
     * @return Optional contenant le produit si trouvé
     */
    Optional<Produit> findById(int id);

    /**
     * Mettre à jour un produit existant
     * @param produit Le produit avec les nouvelles données
     * @return true si succès, false sinon
     */
    boolean update(Produit produit);

    /**
     * Supprimer un produit
     * @param id L'identifiant du produit à supprimer
     * @return true si succès, false sinon
     */
    boolean delete(int id);

    /**
     * Compter le nombre de produits
     * @return Le nombre total de produits
     */
    int count();

    /**
     * Récupérer les produits par catégorie
     * @param categorieId L'identifiant de la catégorie
     * @return Liste des produits de cette catégorie
     */
    List<Produit> findByCategorie(int categorieId);

    /**
     * Rechercher des produits par nom ou description
     * @param searchTerm Le terme de recherche
     * @return Liste des produits correspondants
     */
    List<Produit> search(String searchTerm);

    /**
     * Récupérer les produits disponibles en stock
     * @return Liste des produits disponibles
     */
    List<Produit> findAvailable();

    /**
     * Récupérer les produits en rupture de stock
     * @return Liste des produits en rupture
     */
    List<Produit> findOutOfStock();
}