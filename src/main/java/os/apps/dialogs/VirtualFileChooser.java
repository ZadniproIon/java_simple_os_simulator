package os.apps.dialogs;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import os.vfs.VirtualDirectory;
import os.vfs.VirtualFile;
import os.vfs.VirtualFileSystem;
import os.vfs.VirtualNode;

/**
 * Lightweight dialog that lets the user browse the virtual file system
 * and pick a file. Used by Notepad for opening documents via UI instead
 * of typing raw paths.
 */
public class VirtualFileChooser extends Dialog<VirtualFile> {

    private final VirtualFileSystem fileSystem;
    private VirtualDirectory currentDirectory;
    private final ListView<VirtualNode> listView = new ListView<>();
    private final Label pathLabel = new Label();
    private final ButtonType openButton = new ButtonType("Open", ButtonData.OK_DONE);

    public VirtualFileChooser(VirtualFileSystem fileSystem, VirtualDirectory startDirectory) {
        this.fileSystem = fileSystem;
        this.currentDirectory = startDirectory != null ? startDirectory : fileSystem.getRootDirectory();

        setTitle("Select File");
        setHeaderText("Browse the virtual file system");

        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(VirtualNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof VirtualDirectory dir) {
                    setText("\uD83D\uDCC1  " + dir.getName());
                } else if (item instanceof VirtualFile file) {
                    setText(file.getName());
                } else {
                    setText(item.getName());
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                VirtualNode selected = listView.getSelectionModel().getSelectedItem();
                if (selected instanceof VirtualDirectory dir) {
                    currentDirectory = dir;
                    refreshListing();
                } else if (selected instanceof VirtualFile file) {
                    setResult(file);
                    close();
                }
            }
        });

        Button upButton = new Button("Up");
        upButton.setOnAction(e -> navigateUp());

        HBox header = new HBox(10, new Label("Path:"), pathLabel, upButton);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pathLabel, javafx.scene.layout.Priority.ALWAYS);

        VBox contentBox = new VBox(10, header, listView);
        contentBox.setPadding(new Insets(10));

        BorderPane pane = new BorderPane(contentBox);
        getDialogPane().setContent(pane);
        getDialogPane().getButtonTypes().addAll(openButton, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == openButton) {
                VirtualNode selected = listView.getSelectionModel().getSelectedItem();
                if (selected instanceof VirtualFile file) {
                    return file;
                }
            }
            return null;
        });

        refreshListing();
    }

    private void refreshListing() {
        List<VirtualNode> nodes = fileSystem.list(currentDirectory);
        listView.getItems().setAll(nodes);
        pathLabel.setText(currentDirectory.getPath());
    }

    private void navigateUp() {
        VirtualDirectory parent = currentDirectory.getParent();
        if (parent != null) {
            currentDirectory = parent;
            refreshListing();
        }
    }
}
