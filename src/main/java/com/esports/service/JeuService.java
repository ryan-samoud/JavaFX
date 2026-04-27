package com.esports.service;

import com.esports.interfaces.IJeuService;
import com.esports.model.CategorieJeu;
import com.esports.model.Jeu;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JeuService implements IJeuService {

    private static final Object SCHEMA_LOCK = new Object();
    private static boolean jeuSchemaEnsured = false;

    /**
     * Aligns local DB with the Java model: extra columns + mode enum including {@code coop}.
     * Does not close the shared {@link DatabaseConnection} instance.
     */
    private static void ensureJeuSchema(Connection conn) throws SQLException {
        if (jeuSchemaEnsured) {
            return;
        }
        synchronized (SCHEMA_LOCK) {
            if (jeuSchemaEnsured) {
                return;
            }
            String catalog = conn.getCatalog();
            DatabaseMetaData md = conn.getMetaData();
            Set<String> cols = new HashSet<>();
            try (ResultSet rs = md.getColumns(catalog, null, "jeu", null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            try (Statement st = conn.createStatement()) {
                if (!cols.contains("image")) {
                    st.executeUpdate("ALTER TABLE jeu ADD COLUMN image VARCHAR(512) NULL");
                }
                if (!cols.contains("nb_joueurs")) {
                    st.executeUpdate("ALTER TABLE jeu ADD COLUMN nb_joueurs INT NOT NULL DEFAULT 1");
                }
                if (!cols.contains("note")) {
                    st.executeUpdate("ALTER TABLE jeu ADD COLUMN note DOUBLE NOT NULL DEFAULT 0");
                }
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE jeu MODIFY COLUMN mode ENUM('solo','multi','coop') NOT NULL");
            } catch (SQLException ignored) {
                // Column may already allow coop, or type is not ENUM (e.g. VARCHAR) — ignore
            }
            jeuSchemaEnsured = true;
        }
    }

    @Override
    public boolean add(Jeu j) {
        String sql = "INSERT INTO jeu (nom, tranche_age, mode, description, categorie_id, image, nb_joueurs, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureJeuSchema(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, j.getNom());
                stmt.setInt(2, j.getTrancheAge());
                stmt.setString(3, j.getMode());
                stmt.setString(4, j.getDescription());
                stmt.setInt(5, j.getCategorieId());
                stmt.setString(6, j.getImage());
                stmt.setInt(7, j.getNbJoueurs());
                stmt.setDouble(8, j.getNote());
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[JeuService] add: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(Jeu j) {
        String sql = "UPDATE jeu SET nom = ?, tranche_age = ?, mode = ?, description = ?, categorie_id = ?, image = ?, nb_joueurs = ?, note = ? WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureJeuSchema(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, j.getNom());
                stmt.setInt(2, j.getTrancheAge());
                stmt.setString(3, j.getMode());
                stmt.setString(4, j.getDescription());
                stmt.setInt(5, j.getCategorieId());
                stmt.setString(6, j.getImage());
                stmt.setInt(7, j.getNbJoueurs());
                stmt.setDouble(8, j.getNote());
                stmt.setInt(9, j.getId());
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[JeuService] update: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM jeu WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[JeuService] delete: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Jeu> findAll() {
        List<Jeu> list = new ArrayList<>();
        String sql = "SELECT j.*, c.nom_categorie, c.genre FROM jeu j "
                + "LEFT JOIN categorie_jeu c ON j.categorie_id = c.id";
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureJeuSchema(conn);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                Set<String> labels = new HashSet<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    labels.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
                }
                while (rs.next()) {
                    list.add(mapFull(rs, labels));
                }
            }
        } catch (SQLException e) {
            System.err.println("[JeuService] findAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Jeu findById(int id) {
        String sql = "SELECT j.*, c.nom_categorie, c.genre FROM jeu j "
                + "LEFT JOIN categorie_jeu c ON j.categorie_id = c.id WHERE j.id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureJeuSchema(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    ResultSetMetaData meta = rs.getMetaData();
                    Set<String> labels = new HashSet<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        labels.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
                    }
                    return mapFull(rs, labels);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JeuService] findById: " + e.getMessage());
        }
        return null;
    }

    private Jeu mapFull(ResultSet rs, Set<String> labels) throws SQLException {
        int nbJoueurs = labels.contains("nb_joueurs") ? rs.getInt("nb_joueurs") : 1;
        double note = labels.contains("note") ? rs.getDouble("note") : 0.0;
        String image = labels.contains("image") ? rs.getString("image") : null;

        Jeu j = new Jeu(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getInt("tranche_age"),
                rs.getString("mode"),
                rs.getString("description"),
                rs.getInt("categorie_id"),
                image,
                nbJoueurs,
                note
        );

        int catId = rs.getInt("categorie_id");
        if (catId > 0 && labels.contains("nom_categorie")) {
            String nomCat = rs.getString("nom_categorie");
            if (nomCat != null) {
                CategorieJeu c = new CategorieJeu(
                        catId,
                        nomCat,
                        labels.contains("genre") ? rs.getString("genre") : ""
                );
                j.setCategorie(c);
            }
        }
        return j;
    }
}
