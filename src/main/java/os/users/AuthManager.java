package os.users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple in-memory authentication/authorization component.
 * <p>
 * The GUI or shell is expected to call {@link #login(String, String)}
 * and {@link #logout()} to manage the active user.
 */
public class AuthManager {

    private final List<UserAccount> users = new ArrayList<>();
    private UserAccount currentUser;

    public AuthManager() {
        // Provide a default administrator for convenience.
        addDefaultAdmin();
    }

    /**
     * Adds the default admin account (admin/admin) if not present yet.
     */
    public void addDefaultAdmin() {
        if (users.stream().noneMatch(u -> u.getUsername().equalsIgnoreCase("admin"))) {
            addUser(new UserAccount("admin", "admin", UserRole.ADMIN));
        }
    }

    public void addUser(UserAccount user) {
        Objects.requireNonNull(user, "user");
        users.add(user);
    }

    public List<UserAccount> getUsers() {
        return Collections.unmodifiableList(users);
    }

    /**
     * Attempts to authenticate a user by username and raw password.
     *
     * @return true on success and sets the {@link #currentUser}, false otherwise.
     */
    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        Optional<UserAccount> account = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
        if (account.isPresent() && account.get().checkPassword(password)) {
            currentUser = account.get();
            return true;
        }
        return false;
    }

    /**
     * Logs out the currently authenticated user.
     */
    public void logout() {
        currentUser = null;
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }
}

