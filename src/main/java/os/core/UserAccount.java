package os.core;

/**
 * Simple user description for the login screen.
 */
public class UserAccount {
    private final String username;
    private final String passwordHash;
    private final UserRole role;

    public UserAccount(String username, String passwordHash, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }
}
