package os.apps;

import java.nio.file.Files;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import os.fs.VirtualFile;
import os.fs.VirtualFileSystem;

/**
 * Very small text editor backed by the virtual file system.
 */
public class NotepadApp implements OSApplication {
    private final VirtualFileSystem fileSystem;
    private final VirtualFile initialFile;
    private BorderPane root;
    private TextArea textArea;
    private Text statusText;
    private VirtualFile currentFile;

    public NotepadApp(VirtualFileSystem fileSystem) {
        this(fileSystem, null);
    }

    public NotepadApp(VirtualFileSystem fileSystem, VirtualFile initialFile) {
        this.fileSystem = fileSystem;
        this.initialFile = initialFile;
    }

    @Override
    public String getName() {
        return "Notepad";
    }

    @Override
    public Parent createContent() {
        if (root != null) {
            return root;
        }
        textArea = new TextArea();
        statusText = new Text("Ready");

        MenuItem openItem = new MenuItem("Open...");
        openItem.setOnAction(e -> openFileDialog());
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> saveCurrent());
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> saveAs());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(openItem, saveItem, saveAsItem);

        MenuBar menuBar = new MenuBar(fileMenu);

        BorderPane layout = new BorderPane();
        layout.setTop(menuBar);
        layout.setCenter(textArea);
        BorderPane statusBar = new BorderPane();
        statusBar.setPadding(new Insets(4));
        statusBar.setLeft(statusText);
        layout.setBottom(statusBar);
        root = layout;

        if (initialFile != null) {
            loadFile(initialFile);
        }
        return root;
    }

    private void openFileDialog() {
        TextInputDialog dialog = new TextInputDialog(currentFile != null ?
                fileSystem.getRoot().getPath().relativize(currentFile.getPath()).toString() : "");
        dialog.setTitle("Open File");
        dialog.setHeaderText("Enter a path relative to the virtual root");
        dialog.setContentText("Path:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(path -> {
            try {
                VirtualFile file = fileSystem.resolveFile(path);
                if (!Files.exists(file.getPath())) {
                    showError("File not found");
                    return;
                }
                loadFile(file);
            } catch (Exception ex) {
                showError("Unable to open file");
            }
        });
    }

    private void loadFile(VirtualFile file) {
        try {
            String content = fileSystem.readFile(file);
            currentFile = file;
            textArea.setText(content);
            statusText.setText("Opened " + file.getName());
        } catch (Exception e) {
            showError("Unable to read file");
        }
    }

    private void saveCurrent() {
        if (currentFile == null) {
            saveAs();
            return;
        }
        fileSystem.writeFile(currentFile, textArea.getText());
        statusText.setText("Saved " + currentFile.getName());
    }

    private void saveAs() {
        TextInputDialog dialog = new TextInputDialog(currentFile != null ? currentFile.getName() : "newfile.txt");
        dialog.setTitle("Save As");
        dialog.setHeaderText("Enter a file name relative to the current directory");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        try {
            VirtualFile file = fileSystem.resolveFile(result.get());
            boolean shouldWrite = Files.notExists(file.getPath());
            if (!shouldWrite) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "File exists. Overwrite?", ButtonType.YES, ButtonType.NO);
                alert.setTitle("Confirm overwrite");
                Optional<ButtonType> response = alert.showAndWait();
                shouldWrite = response.isPresent() && response.get() == ButtonType.YES;
            }
            if (shouldWrite) {
                fileSystem.writeFile(file, textArea.getText());
                currentFile = file;
                statusText.setText("Saved " + file.getName());
            }
        } catch (Exception e) {
            showError("Unable to save file");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
