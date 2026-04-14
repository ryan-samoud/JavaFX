package com.esports.service;

import com.esports.model.User;

import java.util.Optional;

public class AuthService {

    private static User currentUser;
    private final UserService userService = new UserService();

    public AuthResult login(String email, String password) {

        if (email == null || email.isBlank())
            return AuthResult.failure("Email vide");

        if (password == null || password.isBlank())
            return AuthResult.failure("Mot de passe vide");

        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty())
            return AuthResult.failure("Email incorrect");

        User user = userOpt.get();

        if (!user.getPassword().equals(password))
            return AuthResult.failure("Mot de passe incorrect");

        currentUser = user;

        System.out.println("✔ LOGIN SUCCESS: " + user.getEmail());

        return AuthResult.success(user);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    // RESULT CLASS
    public static class AuthResult {
        private boolean success;
        private String message;
        private User user;

        public AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, "OK", user);
        }

        public static AuthResult failure(String msg) {
            return new AuthResult(false, msg, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }
}