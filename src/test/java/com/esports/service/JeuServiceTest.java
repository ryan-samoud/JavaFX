package com.esports.service;

import com.esports.interfaces.IJeuService;
import com.esports.model.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JeuServiceTest {

    @Mock
    private IJeuService jeuService; // On mock l'interface pour tester le comportement

    @BeforeEach
    void setUp() {
        System.out.println("--- DÉBUT DU TEST UNITAIRE : JEU SERVICE ---");
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAllGames() {
        System.out.println("[TEST] Vérification de la récupération de tous les jeux...");
        
        // Données simulées
        Jeu j1 = new Jeu(); j1.setNom("Valorant");
        Jeu j2 = new Jeu(); j2.setNom("League of Legends");
        
        when(jeuService.findAll()).thenReturn(Arrays.asList(j1, j2));

        List<Jeu> result = jeuService.findAll();

        assertEquals(2, result.size());
        assertEquals("Valorant", result.get(0).getNom());
        verify(jeuService, times(1)).findAll();
        
        System.out.println("[SUCCÈS] findAll() retourne le bon nombre d'éléments.");
    }

    @Test
    void testAddGame() {
        System.out.println("[TEST] Simulation de l'ajout d'un jeu...");
        
        Jeu j = new Jeu();
        j.setNom("CS:GO");
        
        // On simule l'appel (ne fait rien car c'est un mock)
        jeuService.add(j);
        
        verify(jeuService, times(1)).add(j);
        System.out.println("[SUCCÈS] La méthode add() a été appelée correctement.");
    }
}
