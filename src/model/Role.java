package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * System roles used across BusYatra
 */
public final class Role {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String CLERK = "CLERK";

    private static final Set<String> ROLES = new HashSet<>();

    static {
        ROLES.add(USER);
        ROLES.add(ADMIN);
        ROLES.add(MANAGER);
        ROLES.add(CLERK);
    }

    private Role() {}

    /* ================= ALL ROLES ================= */

    public static Set<String> getAllRoles() {
        return Collections.unmodifiableSet(ROLES);
    }

    /* ================= VALIDATION ================= */

    public static boolean isValidRole(String role) {
        if (role == null) return false;
        return ROLES.contains(role.toUpperCase());
    }

    /* ================= CHECKERS ================= */

    public static boolean isAdmin(String role) {
        return ADMIN.equalsIgnoreCase(role);
    }

    public static boolean isUser(String role) {
        return USER.equalsIgnoreCase(role);
    }

    public static boolean isManager(String role) {
        return MANAGER.equalsIgnoreCase(role);
    }

    public static boolean isClerk(String role) {
        return CLERK.equalsIgnoreCase(role);
    }
}