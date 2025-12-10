package os.apps;

import java.util.Optional;
import java.util.function.IntConsumer;

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
import os.vfs.VirtualFile;
import os.vfs.VirtualFileSystem;

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
    private IntConsumer memoryUsageListener = usage -> {};

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
        textArea.textProperty().addListener((obs, oldText, newText) -> reportMemoryUsage(newText));
        textArea.getStyleClass().add("text-editor");
        statusText = new Text("Ready");
        statusText.getStyleClass().add("status-caption");

        MenuItem openItem = new MenuItem("Open...");
        openItem.setOnAction(e -> openFileDialog());
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> saveCurrent());
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> saveAs());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(openItem, saveItem, saveAsItem);

        MenuBar menuBar = new MenuBar(fileMenu);
        menuBar.getStyleClass().add("top-app-bar");

        BorderPane layout = new BorderPane();
        layout.setTop(menuBar);
        layout.setCenter(textArea);
        BorderPane statusBar = new BorderPane();
        statusBar.setPadding(new Insets(4));
        statusBar.setLeft(statusText);
        statusBar.getStyleClass().add("status-bar");
        layout.setBottom(statusBar);
        root = layout;

        if (initialFile != null) {
            loadFile(initialFile);
        } else {
            reportMemoryUsage(textArea.getText());
        }
        return root;
    }

    public void setMemoryUsageListener(IntConsumer listener) {
        this.memoryUsageListener = listener != null ? listener : usage -> {};
        reportMemoryUsage(textArea != null ? textArea.getText() : "");
    }

    private void openFileDialog() {
        TextInputDialog dialog = new TextInputDialog(currentFile != null ? currentFile.getPath() : "");
        dialog.setTitle("Open File");
        dialog.setHeaderText("Enter a path relative to the virtual root");
        dialog.setContentText("Path:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(path -> {
            try {
                VirtualFile file = fileSystem.resolveFile(path);
                if (!fileSystem.exists(file)) {
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
            reportMemoryUsage(content);
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
        reportMemoryUsage(textArea.getText());
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
            boolean shouldWrite = !fileSystem.exists(file);
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
                reportMemoryUsage(textArea.getText());
            }
        } catch (Exception e) {
            showError("Unable to save file");
        }
    }

    private void reportMemoryUsage(String text) {
        if (text == null) {
            memoryUsageListener.accept(48);
            return;
        }
        int length = text.length();
        int usage = 32 + Math.min(160, length / 1024);
        memoryUsageListener.accept(usage);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
