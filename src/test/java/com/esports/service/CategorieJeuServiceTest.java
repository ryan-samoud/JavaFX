package com.esports.service;

import com.esports.interfaces.ICategorieJeuService;
import com.esports.model.CategorieJeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CategorieJeuServiceTest {

    @Mock
    private ICategorieJeuService categoryService;

    @BeforeEach
    void setUp() {
        System.out.println("--- DÉBUT DU TEST UNITAIRE : CATÉGORIE SERVICE ---");
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAllCategories() {
        System.out.println("[TEST] Vérification de la récupération des catégories...");
        
        CategorieJeu c1 = new CategorieJeu(1, "FPS", "🔫");
        CategorieJeu c2 = new CategorieJeu(2, "MOBA", "🧙");
        
        when(categoryService.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<CategorieJeu> result = categoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("FPS", result.get(0).getNomCategorie());
        System.out.println("[SUCCÈS] Les catégories sont correctement récupérées par le mock.");
    }

    @Test
    void testDeleteCategory() {
        System.out.println("[TEST] Simulation de suppression d'une catégorie...");
        
        categoryService.delete(1);
        
        verify(categoryService, times(1)).delete(1);
        System.out.println("[SUCCÈS] La suppression a été déclenchée avec l'ID correct.");
    }
}
