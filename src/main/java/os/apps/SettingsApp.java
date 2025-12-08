package os.apps;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import os.process.OSKernel;
import os.users.AuthManager;
import os.users.UserAccount;
import os.users.UserRole;

/**
 * Simple settings/control panel. For now it focuses on user management,
 * and can later be extended with appearance and other options.
 */
public class SettingsApp implements OSApplication {

    private final OSKernel kernel;
    private final Runnable onLogoutRequested;
    private BorderPane root;
    private ListView<UserAccount> userListView;

    public SettingsApp(OSKernel kernel, Runnable onLogoutRequested) {
        this.kernel = kernel;
        this.onLogoutRequested = onLogoutRequested;
    }

    @Override
    public String getName() {
        return "Settings";
    }

    @Override
    public Parent createContent() {
        if (root != null) {
            return root;
        }

        AuthManager auth = kernel.getAuthManager();
        UserAccount current = auth.getCurrentUser();

        userListView = new ListView<>();
        userListView.getItems().setAll(auth.getUsers());
        userListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserAccount item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (" + item.getRole() + ")");
                }
            }
        });

        Button addUserButton = new Button("Add User");
        addUserButton.setOnAction(e -> showAddUserDialog());
        Button removeUserButton = new Button("Remove User");
        removeUserButton.setOnAction(e -> removeSelectedUser());
        // Only administrators may create or remove users.
        boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
        if (!isAdmin) {
            addUserButton.setDisable(true);
            removeUserButton.setDisable(true);
        }

        Button logoutButton = new Button("Log out current user");
        logoutButton.setOnAction(e -> {
            auth.logout();
            if (onLogoutRequested != null) {
                onLogoutRequested.run();
            }
        });

        HBox userButtons = new HBox(10, addUserButton, removeUserButton, logoutButton);
        userButtons.setAlignment(Pos.CENTER_LEFT);
        userButtons.setPadding(new Insets(8, 0, 0, 0));

        Label currentUserLabel = new Label(current != null
                ? "Current user: " + current.getUsername() + " (" + current.getRole() + ")"
                : "Current user: <none>");

        Label roleWarning = new Label();
        if (!isAdmin) {
            roleWarning.setText("You are a STANDARD user. Some settings are read-only.");
            roleWarning.setStyle("-fx-text-fill: #ff5555;");
        }

        VBox usersBox = new VBox(6,
                currentUserLabel,
                roleWarning,
                new Label("User accounts"),
                userListView,
                userButtons);
        usersBox.setPadding(new Insets(10));

        root = new BorderPane();
        root.setCenter(usersBox);
        root.setTop(new Label("System Settings"));
        BorderPane.setMargin(root.getTop(), new Insets(8));
        return root;
    }

    private void showAddUserDialog() {
        Dialog<UserAccount> dialog = new Dialog<>();
        dialog.setTitle("Add User");

        Label nameLabel = new Label("Username:");
        TextField nameField = new TextField();
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        Label roleLabel = new Label("Role:");
        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.values());
        roleBox.getSelectionModel().select(UserRole.STANDARD);

        VBox content = new VBox(8,
                new HBox(8, nameLabel, nameField),
                new HBox(8, passwordLabel, passwordField),
                new HBox(8, roleLabel, roleBox));
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String username = nameField.getText();
                String password = passwordField.getText();
                UserRole role = roleBox.getValue();
                if (username == null || username.isBlank() || password == null || password.isBlank()) {
                    return null;
                }
                return new UserAccount(username.trim(), password, role);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(account -> {
            AuthManager auth = kernel.getAuthManager();
            auth.addUser(account);
            userListView.getItems().setAll(auth.getUsers());
        });
    }

    private void removeSelectedUser() {
        UserAccount selected = userListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        AuthManager auth = kernel.getAuthManager();
        UserAccount current = auth.getCurrentUser();
        if (current != null && selected.getUsername().equalsIgnoreCase(current.getUsername())) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "You cannot remove the account you are currently logged in with.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        if ("admin".equalsIgnoreCase(selected.getUsername())) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "The default admin account cannot be removed.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        auth.removeUser(selected.getUsername());
        userListView.getItems().setAll(auth.getUsers());
    }
}
