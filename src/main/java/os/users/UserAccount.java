package os.users;

import java.util.Objects;

/**
 * Represents a user account that can authenticate with the fake OS.
 * <p>
 * Passwords are stored as naive hash strings. This is intentionally
 * insecure and only suitable for teaching/demo purposes.
 */
public class UserAccount {

    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final String homeDirectory;

    public UserAccount(String username, String rawPassword, UserRole role) {
        this(username, rawPassword, role, "/home/" + Objects.requireNonNull(username, "username"));
    }

    public UserAccount(String username, String rawPassword, UserRole role, String homeDirectory) {
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = hashPassword(Objects.requireNonNull(rawPassword, "password"));
        this.role = Objects.requireNonNull(role, "role");
        this.homeDirectory = Objects.requireNonNull(homeDirectory, "homeDirectory");
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

    /**
     * Returns the virtual home directory for this user, e.g. {@code /home/alice}.
     */
    public String getHomeDirectory() {
        return homeDirectory;
    }

    /**
     * Checks a raw password against the stored hash.
     */
    public boolean checkPassword(String rawPassword) {
        return passwordHash.equals(hashPassword(rawPassword));
    }

    private String hashPassword(String password) {
        // Very naive hash function, good enough for a teaching example.
        return Integer.toHexString(password.hashCode());
    }
}
