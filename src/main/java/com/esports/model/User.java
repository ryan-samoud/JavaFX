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
    private String photo;

    public User(int id, String nom, String prenom, String email,
                int age, String role, String password,
                LocalDateTime dateCreation, boolean isActive, String photo) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.age = age;
        this.role = role;
        this.password = password;
        this.dateCreation = dateCreation;
        this.isActive = isActive;
        this.photo = photo;
    }

    // Constructeur sans id (création)
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
    public int getId()                   { return id; }
    public String getNom()               { return nom; }
    public String getPrenom()            { return prenom; }
    public String getEmail()             { return email; }
    public int getAge()                  { return age; }
    public String getRole()              { return role; }
    public String getPassword()          { return password; }
    public LocalDateTime getDateCreation(){ return dateCreation; }
    public boolean isActive()            { return isActive; }
    public String getPhoto()             { return photo; }

    // SETTERS
    public void setNom(String nom)           { this.nom = nom; }
    public void setPrenom(String prenom)     { this.prenom = prenom; }
    public void setEmail(String email)       { this.email = email; }
    public void setAge(int age)              { this.age = age; }
    public void setPassword(String password) { this.password = password; }
    public void setPhoto(String photo)       { this.photo = photo; }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}