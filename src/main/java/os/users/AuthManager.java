package os.users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import os.vfs.VirtualFile;
import os.vfs.VirtualFileSystem;

/**
 * Simple in-memory authentication/authorization component.
 * <p>
 * The GUI or shell is expected to call {@link #login(String, String)}
 * and {@link #logout()} to manage the active user.
 */
public class AuthManager {

    private final List<UserAccount> users = new ArrayList<>();
    private UserAccount currentUser;
    private final VirtualFileSystem fileSystem;
    private final VirtualFile userStoreFile;

    /**
     * Creates an AuthManager that persists user accounts into the given
     * virtual file system (under /etc/users.db).
     */
    public AuthManager(VirtualFileSystem fileSystem) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem");
        this.userStoreFile = fileSystem.resolveFile("/etc/users.db");
        loadUsers();
        // Provide a default administrator for convenience.
        addDefaultAdmin();
    }

    /**
     * Adds the default admin account (admin/admin) if not present yet.
     */
    public void addDefaultAdmin() {
        if (users.stream().noneMatch(u -> u.getUsername().equalsIgnoreCase("admin"))) {
            users.add(new UserAccount("admin", "admin", UserRole.ADMIN));
            saveUsers();
        }
    }

    public void addUser(UserAccount user) {
        Objects.requireNonNull(user, "user");
        users.add(user);
        saveUsers();
    }

    /**
     * Removes a user by username. The default admin account is preserved.
     */
    public void removeUser(String username) {
        if (username == null) {
            return;
        }
        if ("admin".equalsIgnoreCase(username)) {
            return;
        }
        users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
        saveUsers();
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

    private void loadUsers() {
        try {
            if (!fileSystem.exists(userStoreFile)) {
                return;
            }
            String content = fileSystem.readFile(userStoreFile);
            if (content.isBlank()) {
                return;
            }
            String[] lines = content.split("\\R");
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(";");
                if (parts.length < 4) {
                    continue;
                }
                String username = parts[0];
                String hash = parts[1];
                UserRole role = UserRole.valueOf(parts[2]);
                String home = parts[3];
                users.add(UserAccount.fromHashed(username, hash, role, home));
            }
        } catch (Exception ignored) {
            // If loading fails we simply fall back to a fresh user list.
        }
    }

    private void saveUsers() {
        StringBuilder sb = new StringBuilder();
        for (UserAccount user : users) {
            sb.append(user.getUsername()).append(";")
              .append(user.getPasswordHash()).append(";")
              .append(user.getRole().name()).append(";")
              .append(user.getHomeDirectory()).append("\n");
        }
        fileSystem.writeFile(userStoreFile, sb.toString());
    }
}
