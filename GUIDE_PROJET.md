# Guide Technique — NexUS Gaming Arena
## JavaFX vs Symfony · CRUD · Ban/Suspend · Hachage · Contrôle de saisie

---

## 1. Architecture du projet (MVC)

```
src/
├── model/          → Les entités (User, Produit, Tournoi…)
├── service/        → La logique métier (UserService, AuthService…)
├── interfaces/     → Les contrats de service (IUserService…)
├── controller/     → Les contrôleurs JavaFX (UsersController…)
├── utils/          → Outils transversaux (DatabaseConnection, PasswordUtil)
└── resources/fxml/ → Les vues (UsersView.fxml, HomeView.fxml…)
```

| Couche       | Java/JavaFX          | Symfony/Web           |
|--------------|----------------------|-----------------------|
| Vue          | Fichier `.fxml`      | Fichier `.twig`       |
| Contrôleur   | `*Controller.java`   | `*Controller.php`     |
| Modèle       | `*Service.java`      | `Repository` + Entity |
| Base données | JDBC + SQL à la main | Doctrine ORM          |
| Routing      | Navigation de scène  | `#[Route]`            |

---

## 2. CRUD — Comment ça marche ici

### CREATE (Créer un utilisateur)

**Java (UserService.java) :**
```java
public boolean save(User user) {
    String sql = "INSERT INTO user (nom, prenom, email, age, role, password, is_active, date_creation)
                  VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";
    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setString(1, user.getNom());
    // ... on passe les valeurs manuellement
    stmt.setString(6, PasswordUtil.hash(user.getPassword())); // haché avant insertion
    return stmt.executeUpdate() > 0;
}
```

**Symfony (équivalent) :**
```php
$user = new User();
$user->setPassword($hasher->hashPassword($user, $plainPassword));
$em->persist($user);
$em->flush();
```

> **Différence clé :** en Symfony, Doctrine gère le SQL automatiquement.
> En Java, on écrit chaque requête à la main avec `PreparedStatement`.

---

### READ (Lire les utilisateurs)

**Java :**
```java
public List<User> findAllUsers() {
    String sql = "SELECT * FROM user ORDER BY date_creation DESC";
    ResultSet rs = stmt.executeQuery();
    while (rs.next()) list.add(map(rs)); // on convertit chaque ligne en objet User
    return list;
}
```

La méthode `map(ResultSet rs)` reconstruit un objet Java depuis les colonnes SQL :
```java
return new User(
    rs.getInt("id"),
    rs.getString("nom"),
    rs.getString("email"),
    // ...
);
```

**Symfony :**
```php
$users = $userRepository->findAll(); // Doctrine fait tout
```

---

### UPDATE (Modifier un utilisateur)

**Java :**
```java
public boolean update(User user) {
    String sql = "UPDATE user SET nom=?, prenom=?, email=?, password=? WHERE id=?";
    stmt.setString(1, user.getNom());
    stmt.setString(4, PasswordUtil.isHashed(user.getPassword())
        ? user.getPassword()          // déjà haché → on garde
        : PasswordUtil.hash(user.getPassword())); // sinon on hache
}
```

**Symfony :**
```php
$user->setNom($newNom);
$em->flush(); // Doctrine détecte les changements automatiquement
```

---

### DELETE / DEACTIVATE (Supprimer / Désactiver)

Dans ce projet on utilise le **soft delete** : on ne supprime pas vraiment,
on met `is_active = 0`.

```java
public boolean deactivate(int id) {
    String sql = "UPDATE user SET is_active = 0 WHERE id = ?";
}
```

Cela permet de conserver les données et de les restaurer plus tard.

---

## 3. Ban & Suspension — Fonctionnement

### Colonnes en base de données

```sql
ALTER TABLE user ADD COLUMN ban_reason VARCHAR(500) NULL;
ALTER TABLE user ADD COLUMN suspended_until DATETIME NULL;
```

Ces colonnes sont créées automatiquement au démarrage si elles n'existent pas
(méthode `ensureColumns()` dans `UserService`).

### Logique des statuts

| `is_active` | `suspended_until`      | Statut          |
|-------------|------------------------|-----------------|
| `1`         | `NULL`                 | ✅ Actif        |
| `0`         | `NULL`                 | 🚫 Banni        |
| `0`         | date future            | ⏸ Suspendu      |

### Ban (permanent)
```java
public boolean ban(int id, String reason) {
    "UPDATE user SET is_active=0, ban_reason=?, suspended_until=NULL WHERE id=?"
}
```

### Suspension (temporaire)
```java
public boolean suspend(int id, LocalDateTime until, String reason) {
    "UPDATE user SET is_active=0, suspended_until=?, ban_reason=? WHERE id=?"
}
```

### Débannir / Lever suspension
```java
public boolean unban(int id) {
    "UPDATE user SET is_active=1, ban_reason=NULL, suspended_until=NULL WHERE id=?"
}
```

### Détection dans le modèle (User.java)
```java
public boolean isBanned() {
    return !isActive && suspendedUntil == null;
}
public boolean isSuspended() {
    return !isActive && suspendedUntil != null
           && suspendedUntil.isAfter(LocalDateTime.now());
}
```

### Au moment du login (AuthService.java)
```java
if (user.isBanned())     return AuthResult.banned(user.getBanReason());
if (user.isSuspended())  return AuthResult.suspended(user.getBanReason(), user.getSuspendedUntil());
```
→ Une fenêtre stylisée s'affiche avec la raison et un **countdown en direct**
  si le compte est suspendu.

---

## 4. Hachage des mots de passe (BCrypt)

### Pourquoi BCrypt ?
- Chaque hash est **unique** même pour le même mot de passe (grâce au salt)
- **Irréversible** : impossible de retrouver le mot de passe depuis le hash
- `cost=12` : lent volontairement pour résister aux attaques brute-force

### PasswordUtil.java
```java
// Hacher un mot de passe
String hash = PasswordUtil.hash("monMotDePasse");
// → "$2a$12$xK9......" (60 caractères)

// Vérifier à la connexion
boolean ok = PasswordUtil.verify("monMotDePasse", hashStockeEnBase);
```

### Rétro-compatibilité
Les anciens comptes avec mot de passe en clair continuent de fonctionner :
```java
public static boolean verify(String plain, String stored) {
    if (stored.startsWith("$2a$") || stored.startsWith("$2b$")) {
        return BCrypt.checkpw(plain, stored); // compte haché
    }
    return stored.equals(plain); // ancien compte en clair
}
```

### Comparaison avec Symfony
```php
// Symfony — tout est géré par le framework
$hasher->hashPassword($user, $plain);
$hasher->isPasswordValid($user, $plain);
```
```java
// Java — on utilise la librairie jbcrypt manuellement
BCrypt.hashpw(plain, BCrypt.gensalt(12));
BCrypt.checkpw(plain, stored);
```

---

## 5. Contrôle de saisie (Validation)

En Symfony on utilise les annotations `#[Assert\NotBlank]`, `#[Assert\Email]`…
En Java/JavaFX, la validation se fait **manuellement** dans le contrôleur.

### Exemple — RegisterController.java
```java
// Champ vide
if (nom.isEmpty()) {
    errNom.setText("Le nom est obligatoire.");
    valid = false;
}

// Format email
if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
    errEmail.setText("Email invalide.");
    valid = false;
}

// Mot de passe minimum
if (password.length() < 8) {
    errPassword.setText("Minimum 8 caractères.");
    valid = false;
}

// Confirmation mot de passe
if (!password.equals(confirm)) {
    errConfirm.setText("Les mots de passe ne correspondent pas.");
    valid = false;
}

// Âge
if (age < 13 || age > 99) {
    errAge.setText("Âge invalide (13-99).");
    valid = false;
}

if (!valid) return; // on arrête si une erreur existe
```

---

## 6. Comment JavaFX fonctionne (vs Web)

### Le cycle de vie d'une vue

```
Web (Symfony)                    JavaFX
─────────────────                ──────────────────────
1. Requête HTTP                  1. Clic sur un bouton
2. Route → Contrôleur            2. @FXML void onAction()
3. Rendu Twig → HTML             3. FXMLLoader charge le .fxml
4. Réponse envoyée               4. La scène est remplacée
```

### Liaison FXML ↔ Contrôleur
```xml
<!-- UsersView.fxml -->
<TextField fx:id="fieldSearch" />
<Button onAction="#onAddUser" text="Ajouter" />
```
```java
// UsersController.java
@FXML private TextField fieldSearch;  // injection automatique par le nom

@FXML
private void onAddUser() {            // appelé au clic
    // ...
}
```

### Sessions utilisateur
```php
// Symfony — géré par le framework
$this->getUser(); // retourne l'utilisateur connecté
```
```java
// Java — on gère soi-même avec une variable statique
AuthService.getCurrentUser();  // retourne le User connecté
AuthService.logout();           // efface la session
```

### Affichage dynamique (TableView)
```java
// On lie les données à la table
ObservableList<User> list = FXCollections.observableArrayList(users);
tableUsers.setItems(list);

// Quand on modifie la liste, le tableau se met à jour automatiquement
list.setAll(newUsers); // → la table se rafraîchit
```

---

## 7. Résumé des fichiers importants

| Fichier                      | Rôle                                      |
|------------------------------|-------------------------------------------|
| `User.java`                  | Modèle utilisateur (champs + getters)     |
| `UserService.java`           | CRUD + ban/suspend en base de données     |
| `AuthService.java`           | Login, session, détection ban/suspend     |
| `PasswordUtil.java`          | Hachage BCrypt + rétro-compat             |
| `UsersController.java`       | Page admin : liste, ajout, ban, suspend   |
| `LoginController.java`       | Formulaire login + fenêtres ban/suspend   |
| `RegisterController.java`    | Formulaire inscription + validation       |
| `UsersView.fxml`             | Interface de la page gestion utilisateurs |
| `dashboard.css`              | Thème NexUS (violet/rose/dark)            |
| `module-info.java`           | Déclaration des modules Java requis       |
