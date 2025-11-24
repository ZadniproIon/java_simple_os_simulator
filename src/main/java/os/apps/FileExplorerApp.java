package os.apps;

import java.util.ArrayList;
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
import os.fs.VirtualDirectory;
import os.fs.VirtualFile;
import os.fs.VirtualFileSystem;

/**
 * Minimal file browser over the VirtualFileSystem.
 */
public class FileExplorerApp implements OSApplication {
    private final VirtualFileSystem fileSystem;
    private final Consumer<VirtualFile> textFileOpener;
    private VirtualDirectory currentDirectory;
    private BorderPane root;
    private ListView<Object> listView;
    private Label pathLabel;

    public FileExplorerApp(VirtualFileSystem fileSystem, Consumer<VirtualFile> textFileOpener) {
        this.fileSystem = fileSystem;
        this.textFileOpener = textFileOpener;
        this.currentDirectory = fileSystem.getRoot();
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
        toolbar.setPadding(new Insets(10));

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(listView);
        root.setLeft(leftPane);

        refreshListing();
        return root;
    }

    private void refreshListing() {
        List<Object> entries = new ArrayList<>();
        entries.addAll(fileSystem.listDirectories(currentDirectory));
        entries.addAll(fileSystem.listFiles(currentDirectory));
        ObservableList<Object> observable = FXCollections.observableArrayList(entries);
        listView.setItems(observable);
        String relative = fileSystem.getRoot().getPath().relativize(currentDirectory.getPath()).toString();
        if (relative.isEmpty()) {
            relative = "/";
        }
        pathLabel.setText(relative);
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
        if (currentDirectory.getPath().equals(fileSystem.getRoot().getPath())) {
            return;
        }
        currentDirectory = new VirtualDirectory(currentDirectory.getPath().getParent());
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
