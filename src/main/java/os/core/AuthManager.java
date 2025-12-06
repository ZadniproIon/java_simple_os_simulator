package os.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory authentication service for the fake OS.
 */
public class AuthManager {
    private final Map<String, UserAccount> users = new HashMap<>();
    private UserAccount currentUser;

    public AuthManager() {
        addUser("admin", "admin", UserRole.ADMIN);
        addUser("guest", "guest", UserRole.STANDARD);
    }

    public void addUser(String username, String password, UserRole role) {
        String hash = hashPassword(password);
        users.put(username.toLowerCase(), new UserAccount(username, hash, role));
    }

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        UserAccount account = users.get(username.toLowerCase());
        if (account != null && account.getPasswordHash().equals(hashPassword(password))) {
            currentUser = account;
            return true;
        }
        return false;
    }

    public Optional<UserAccount> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }
}
