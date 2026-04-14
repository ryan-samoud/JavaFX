# ⚡ NEXUS ESPORTS — Application JavaFX MVC

Application de gestion esports construite avec JavaFX en suivant l'architecture **MVC**.

---

## 📁 Structure du Projet (MVC)

```
esports-app/
├── pom.xml                                         ← Config Maven + dépendances
└── src/main/
    ├── java/com/esports/
    │   ├── MainApp.java                            ← 🚀 Point d'entrée JavaFX
    │   │
    │   ├── model/                                  ← 📦 MODÈLE (données)
    │   │   ├── Player.java                         ←   Entité Joueur (JavaFX Properties)
    │   │   └── Team.java                           ←   Entité Équipe
    │   │
    │   ├── view/                                   ← 🖼️  VUE (FXML = la vue)
    │   │   └── (vos vues FXML supplémentaires)
    │   │
    │   ├── controller/                             ← 🎮 CONTRÔLEUR (logique)
    │   │   └── MainController.java                 ←   Lien Vue ↔ Modèle
    │   │
    │   └── util/                                   ← 🛠️  Utilitaires
    │       └── (helpers, converters, validators)
    │
    └── resources/
        ├── fxml/
        │   └── MainView.fxml                       ← 🖥️  Interface principale
        ├── css/
        │   └── esports-theme.css                   ← 🎨 Thème dark esports
        └── images/
            └── (logos, icônes)
```

---

## 🚀 Lancer l'application

### Prérequis
- Java 17+
- Maven 3.8+

### Commandes
```bash
# Compiler
mvn clean compile

# Lancer
mvn javafx:run

# Package JAR
mvn clean package
```

---

## 🏗️ Architecture MVC expliquée

| Couche | Fichier | Rôle |
|--------|---------|------|
| **Model** | `Player.java`, `Team.java` | Données + JavaFX Properties pour le binding |
| **View** | `MainView.fxml` + CSS | Interface déclarative (aucune logique) |
| **Controller** | `MainController.java` | Reçoit les events, met à jour le Model et la View |

### Flux de données
```
Utilisateur → View (FXML) → Controller → Model → View (binding auto)
```

---

## 📋 Prochaines étapes suggérées

1. **Ajouter des vues FXML** pour Équipes, Tournois, Stats
2. **Couche Service** : `PlayerService.java`, `TeamService.java`  
3. **Couche DAO** : Connexion Base de données (MySQL/SQLite)
4. **Animations** : Transitions entre les sections
5. **Authentification** : Fenêtre de login

---

## 🎨 Thème

- Palette : **Dark Navy** `#0a0d14` + **Cyan Neon** `#00e5ff` + **Purple** `#a855f7`
- Badges statut : 🟢 Online · 🔴 In-Game · ⚫ Offline
# JavaFX
