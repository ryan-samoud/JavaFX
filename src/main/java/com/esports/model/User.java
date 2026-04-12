package com.esports.model;

import java.time.LocalDateTime;

public class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private int age;
    private String role;
    private String password;
    private LocalDateTime dateCreation;
    private boolean isActive;

    public User(int id, String nom, String prenom, String email,
                int age, String role, String password,
                LocalDateTime dateCreation, boolean isActive) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        this.role = role;
        this.password = password;
        this.dateCreation = dateCreation;
        this.isActive = isActive;
    }

    public User(String nom, String prenom, String email,
                int age, String role, String password) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        this.role = role;
        this.password = password;
        this.isActive = true;
    }

    // GETTERS
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
    public String getRole() { return role; }
    public String getPassword() { return password; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public boolean isActive() { return isActive; }

    // helper
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}