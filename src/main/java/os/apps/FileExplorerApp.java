package os.apps;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import os.users.UserAccount;
import os.users.UserRole;
import os.vfs.VirtualDirectory;
import os.vfs.VirtualFile;
import os.vfs.VirtualFileSystem;

/**
 * Minimal file browser over the VirtualFileSystem.
 */
public class FileExplorerApp implements OSApplication {
    private final VirtualFileSystem fileSystem;
    private final Consumer<VirtualFile> textFileOpener;
    private final VirtualDirectory rootDirectory;
    private final UserAccount currentUser;
    private final boolean isAdmin;
    private VirtualDirectory currentDirectory;
    private BorderPane root;
    private ListView<Object> listView;
    private Label pathLabel;

    public FileExplorerApp(VirtualFileSystem fileSystem,
                           VirtualDirectory rootDirectory,
                           UserAccount currentUser,
                           Consumer<VirtualFile> textFileOpener) {
        this.fileSystem = fileSystem;
        this.textFileOpener = textFileOpener;
        this.rootDirectory = rootDirectory != null ? rootDirectory : fileSystem.getRootDirectory();
        this.currentDirectory = this.rootDirectory;
        this.currentUser = currentUser;
        this.isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    @Override
    public String getName() {
        return "File Explorer";
    }

    @Override
    public Parent createContent() {
        if (root != null) {
            return root;
        }
        listView = new ListView<>();
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof VirtualDirectory dir) {
                    setText("[DIR] " + dir.getName());
                } else if (item instanceof VirtualFile file) {
                    setText(file.getName());
                }
            }
        });
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Object selected = listView.getSelectionModel().getSelectedItem();
                if (selected instanceof VirtualDirectory dir) {
                    currentDirectory = dir;
                    refreshListing();
                } else if (selected instanceof VirtualFile file) {
                    openFile(file);
                }
            }
        });

        Button upButton = new Button("Up");
        upButton.setOnAction(e -> navigateUp());
        Button newFileButton = new Button("New File");
        newFileButton.setOnAction(e -> createNewFile());
        Button newDirButton = new Button("New Folder");
        newDirButton.setOnAction(e -> createNewDirectory());
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> deleteSelected());

        pathLabel = new Label();
        VBox leftPane = new VBox(10, new Label("Current Path:"), pathLabel);
        leftPane.setPadding(new Insets(10));

        HBox toolbar = new HBox(10, upButton, newFileButton, newDirButton, deleteButton);
        toolbar.setPadding(new Insets(4, 10, 10, 10));

        Label roleLabel = new Label();
        if (isAdmin) {
            roleLabel.setText("Admin: full access to all user files.");
        } else if (currentUser != null) {
            roleLabel.setText("Standard user: access limited to your home directory ("
                    + currentUser.getUsername() + ").");
        } else {
            roleLabel.setText("Guest mode: limited access.");
        }
        roleLabel.setStyle("-fx-text-fill: #ffcc66; -fx-font-size: 11px;");

        VBox topBox = new VBox(2, roleLabel, toolbar);
        topBox.setPadding(new Insets(8, 0, 0, 0));

        root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(listView);
        root.setLeft(leftPane);

        refreshListing();
        return root;
    }

    private void refreshListing() {
        List<Object> entries = fileSystem.list(currentDirectory).stream()
                .map(node -> (Object) node)
                .toList();
        ObservableList<Object> observable = FXCollections.observableArrayList(entries);
        listView.setItems(observable);
        updatePathLabel();
    }

    private void updatePathLabel() {
        String rootPath = rootDirectory.getPath();
        String currentPath = currentDirectory.getPath();
        String display;
        if (currentPath.equals(rootPath)) {
            display = "/";
        } else if (currentPath.startsWith(rootPath)) {
            String rel = currentPath.substring(rootPath.length());
            if (rel.isEmpty()) {
                rel = "/";
            }
            if (!rel.startsWith("/")) {
                rel = "/" + rel;
            }
            display = rel;
        } else {
            display = currentPath;
        }
        pathLabel.setText(display);
    }

    private void openFile(VirtualFile file) {
        if (textFileOpener != null && file.getName().endsWith(".txt")) {
            textFileOpener.accept(file);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "No viewer registered for this file type");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void navigateUp() {
        if (currentDirectory == rootDirectory) {
            return;
        }
        VirtualDirectory parent = currentDirectory.getParent();
        if (parent != null) {
            currentDirectory = parent;
        }
        refreshListing();
    }

    private void createNewFile() {
        TextInputDialog dialog = new TextInputDialog("newfile.txt");
        dialog.setTitle("New File");
        dialog.setHeaderText("Enter file name");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                fileSystem.createFile(currentDirectory, name);
                refreshListing();
            } catch (Exception ex) {
                showError("Unable to create file");
            }
        });
    }

    private void createNewDirectory() {
        TextInputDialog dialog = new TextInputDialog("folder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Enter folder name");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                fileSystem.createDirectory(currentDirectory, name);
                refreshListing();
            } catch (Exception ex) {
                showError("Unable to create folder");
            }
        });
    }

    private void deleteSelected() {
        Object selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            if (selected instanceof VirtualFile file) {
                fileSystem.delete(file);
            } else if (selected instanceof VirtualDirectory directory) {
                fileSystem.delete(directory);
            }
            refreshListing();
        } catch (Exception e) {
            showError("Unable to delete entry");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
