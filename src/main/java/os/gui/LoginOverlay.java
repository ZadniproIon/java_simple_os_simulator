package os.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import os.process.OSKernel;
import os.users.AuthManager;
import os.users.UserAccount;

/**
 * Simple full-screen login overlay shown on top of the desktop.
 * <p>
 * Users select an account from a list (similar to Windows) and then
 * enter the password for that account.
 */
public class LoginOverlay extends BorderPane {

    private final OSKernel kernel;
    private final AuthManager authManager;
    private final Runnable onLoginSuccess;

    private final ComboBox<UserAccount> userBox = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label("Select a user and enter the password");

    public LoginOverlay(OSKernel kernel, Runnable onLoginSuccess) {
        this.kernel = kernel;
        this.authManager = kernel.getAuthManager();
        this.onLoginSuccess = onLoginSuccess;

        setStyle("-fx-background-color: #202020;");
        setPadding(new Insets(40));

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color: #2d2d2d; -fx-background-radius: 8;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 4);");

        Label title = new Label("Welcome");
        title.setFont(Font.font(20));
        title.setTextFill(Color.WHITE);

        refreshUsers();
        userBox.setPromptText("Select user");
        userBox.setCellFactory(list -> new UserCell());
        userBox.setButtonCell(new UserCell());

        passwordField.setPromptText("Password");

        Button loginButton = new Button("Sign in");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> attemptLogin());

        statusLabel.setTextFill(Color.LIGHTGRAY);

        card.getChildren().addAll(title, userBox, passwordField, loginButton, statusLabel);
        setCenter(card);
        BorderPane.setAlignment(card, Pos.CENTER);
    }

    /**
     * Reloads the user list from the {@link AuthManager}. Call this before
     * showing the overlay to pick up any newly created users.
     */
    public void refreshUsers() {
        userBox.getItems().setAll(authManager.getUsers());
        if (!userBox.getItems().isEmpty()) {
            userBox.getSelectionModel().selectFirst();
        }
    }

    private void attemptLogin() {
        UserAccount selected = userBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a user.");
            return;
        }
        boolean ok = authManager.login(selected.getUsername(), passwordField.getText());
        if (ok) {
            kernel.ensureCurrentUserHomeDirectory();
            passwordField.clear();
            statusLabel.setText("Signed in as " + selected.getUsername());
            setVisible(false);
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } else {
            statusLabel.setText("Incorrect password for " + selected.getUsername());
            passwordField.clear();
        }
    }

    /**
     * Makes the overlay visible and resets fields, ready for a new login.
     */
    public void showOverlay() {
        refreshUsers();
        passwordField.clear();
        statusLabel.setText("Select a user and enter the password");
        setVisible(true);
    }

    /**
     * Custom cell that shows a small avatar block + username.
     */
    private static class UserCell extends javafx.scene.control.ListCell<UserAccount> {
        @Override
        protected void updateItem(UserAccount item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                Rectangle avatar = new Rectangle(24, 24, Color.DODGERBLUE);
                avatar.setArcWidth(8);
                avatar.setArcHeight(8);
                Label name = new Label(item.getUsername() + " (" + item.getRole() + ")");
                name.setTextFill(Color.WHITE);
                VBox box = new VBox(name);
                box.setSpacing(2);
                box.setPadding(new Insets(2, 0, 0, 8));
                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(avatar, box);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setSpacing(8);
                setGraphic(row);
                setText(null);
            }
        }
    }
}
