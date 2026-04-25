package com.esports.service;

import com.esports.model.User;
import com.esports.utils.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {

    private static User currentUser;
    private final UserService userService = new UserService();

    public AuthResult login(String email, String password) {

        if (email == null || email.isBlank())
            return AuthResult.failure("Email vide");

        if (password == null || password.isBlank())
            return AuthResult.failure("Mot de passe vide");

        // Check any user (including banned/suspended) first
        Optional<User> anyOpt = userService.findByEmailAny(email);

        if (anyOpt.isEmpty())
            return AuthResult.failure("Email incorrect");

        User user = anyOpt.get();

        if (!PasswordUtil.verify(password, user.getPassword()))
            return AuthResult.failure("Mot de passe incorrect");

        // Banned
        if (user.isBanned())
            return AuthResult.banned(user.getBanReason());

        // Suspended
        if (user.isSuspended())
            return AuthResult.suspended(user.getBanReason(), user.getSuspendedUntil());

        currentUser = user;
        System.out.println("✔ LOGIN SUCCESS: " + user.getEmail());
        return AuthResult.success(user);
    }

    public static User getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn()  { return currentUser != null; }
    public static void logout()         { currentUser = null; }

    // ─────────────────────────────────────────────────────
    // RESULT
    // ─────────────────────────────────────────────────────
    public static class AuthResult {

        public enum Status { SUCCESS, FAILURE, BANNED, SUSPENDED }

        private final Status        status;
        private final String        message;
        private final User          user;
        private final String        banReason;
        private final LocalDateTime suspendedUntil;

        private AuthResult(Status status, String message, User user,
                           String banReason, LocalDateTime suspendedUntil) {
            this.status         = status;
            this.message        = message;
            this.user           = user;
            this.banReason      = banReason;
            this.suspendedUntil = suspendedUntil;
        }

        public static AuthResult success(User user) {
            return new AuthResult(Status.SUCCESS, "OK", user, null, null);
        }
        public static AuthResult failure(String msg) {
            return new AuthResult(Status.FAILURE, msg, null, null, null);
        }
        public static AuthResult banned(String reason) {
            return new AuthResult(Status.BANNED, "Compte banni", null, reason, null);
        }
        public static AuthResult suspended(String reason, LocalDateTime until) {
            return new AuthResult(Status.SUSPENDED, "Compte suspendu", null, reason, until);
        }

        public boolean isSuccess()              { return status == Status.SUCCESS; }
        public Status  getStatus()              { return status; }
        public String  getMessage()             { return message; }
        public User    getUser()                { return user; }
        public String  getBanReason()           { return banReason; }
        public LocalDateTime getSuspendedUntil(){ return suspendedUntil; }
    }
}
