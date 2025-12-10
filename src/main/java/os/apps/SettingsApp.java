package os.apps;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import os.gui.wallpaper.WallpaperService;
import os.gui.wallpaper.WallpaperService.WallpaperOption;
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
    private final WallpaperService wallpaperService;
    private final Runnable onWallpaperChanged;
    private BorderPane root;
    private ListView<UserAccount> userListView;
    private ComboBox<WallpaperOption> wallpaperBox;
    private ImageView wallpaperPreview;

    public SettingsApp(OSKernel kernel,
                       Runnable onLogoutRequested,
                       WallpaperService wallpaperService,
                       Runnable onWallpaperChanged) {
        this.kernel = kernel;
        this.onLogoutRequested = onLogoutRequested;
        this.wallpaperService = wallpaperService;
        this.onWallpaperChanged = onWallpaperChanged;
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
        userListView.setPrefHeight(260);
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
        addUserButton.getStyleClass().add("outlined-button");
        addUserButton.setOnAction(e -> showAddUserDialog());
        Button removeUserButton = new Button("Remove User");
        removeUserButton.getStyleClass().add("outlined-button");
        removeUserButton.setOnAction(e -> removeSelectedUser());
        // Only administrators may create or remove users.
        boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
        if (!isAdmin) {
            addUserButton.setDisable(true);
            removeUserButton.setDisable(true);
        }

        Button logoutButton = new Button("Log out current user");
        logoutButton.getStyleClass().add("primary-button");
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
        currentUserLabel.getStyleClass().add("section-title");

        Label roleWarning = new Label();
        if (!isAdmin) {
            roleWarning.setText("You are a STANDARD user. Some settings are read-only.");
            roleWarning.getStyleClass().add("warning-text");
        }

        VBox appearanceBox = buildAppearanceBox(current);

        VBox usersBox = new VBox(6,
                currentUserLabel,
                roleWarning,
                new Label("User accounts"),
                userListView,
                userButtons);
        usersBox.setPadding(new Insets(10));
        usersBox.getStyleClass().add("card");
        usersBox.setPrefWidth(330);
        VBox.setVgrow(userListView, javafx.scene.layout.Priority.ALWAYS);

        HBox contentRow = new HBox(24, usersBox, appearanceBox);
        contentRow.setPadding(new Insets(10));
        contentRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(usersBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(appearanceBox, javafx.scene.layout.Priority.NEVER);

        root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setCenter(contentRow);
        Label title = new Label("System Settings");
        title.getStyleClass().add("section-title");
        root.setTop(title);
        BorderPane.setMargin(title, new Insets(0, 0, 8, 0));
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

    private VBox buildAppearanceBox(UserAccount current) {
        Label title = new Label("Appearance");
        title.setStyle("-fx-font-weight: bold;");

        Label subtitle = new Label(current != null
                ? "Desktop wallpaper for " + current.getUsername()
                : "Log in to customize wallpapers.");

        wallpaperBox = new ComboBox<>();
        wallpaperBox.setPrefWidth(320);
        wallpaperBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(WallpaperOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        });
        wallpaperBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WallpaperOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        });
        wallpaperBox.getItems().setAll(wallpaperService.getWallpaperOptions());
        WallpaperOption initial = current != null
                ? findOption(current.getPreferredWallpaper())
                : null;
        if (initial != null) {
            wallpaperBox.getSelectionModel().select(initial);
        } else if (!wallpaperBox.getItems().isEmpty()) {
            wallpaperBox.getSelectionModel().selectFirst();
        }

        wallpaperPreview = new ImageView();
        wallpaperPreview.setFitWidth(220);
        wallpaperPreview.setPreserveRatio(true);
        wallpaperPreview.setSmooth(true);
        wallpaperPreview.setStyle("-fx-border-color: #444; -fx-border-radius: 4;");
        updatePreview(wallpaperBox.getSelectionModel().getSelectedItem());
        wallpaperBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview(newVal));

        Button applyButton = new Button("Apply wallpaper");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setDisable(current == null);
        applyButton.setOnAction(e -> applyWallpaperSelection(current));

        VBox appearance = new VBox(8, title, subtitle, wallpaperBox, wallpaperPreview, applyButton);
        appearance.setPadding(new Insets(12));
        appearance.setPrefWidth(260);
        appearance.getStyleClass().add("card");
        return appearance;
    }

    private void updatePreview(WallpaperOption option) {
        if (option == null) {
            wallpaperPreview.setImage(null);
            return;
        }
        wallpaperPreview.setImage(wallpaperService.loadPreview(option.fileName(), 220, 130));
    }

    private WallpaperOption findOption(String fileName) {
        if (fileName == null) {
            return null;
        }
        for (WallpaperOption option : wallpaperBox.getItems()) {
            if (option.fileName().equalsIgnoreCase(fileName)) {
                return option;
            }
        }
        return null;
    }

    private void applyWallpaperSelection(UserAccount current) {
        WallpaperOption option = wallpaperBox.getSelectionModel().getSelectedItem();
        if (current == null || option == null) {
            return;
        }
        current.setPreferredWallpaper(option.fileName());
        kernel.getAuthManager().updateUser(current);
        if (onWallpaperChanged != null) {
            onWallpaperChanged.run();
        }
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
