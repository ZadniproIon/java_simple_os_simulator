package os.apps;

import java.util.Optional;
import java.util.function.IntConsumer;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import os.apps.dialogs.VirtualFileChooser;
import os.vfs.VirtualDirectory;
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
    private boolean dirty;
    private boolean suppressTextEvents;

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
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!suppressTextEvents) {
                dirty = true;
            }
            reportMemoryUsage(newText);
        });
        textArea.getStyleClass().add("text-editor");
        statusText = new Text("Ready");
        statusText.getStyleClass().add("status-caption");

        MenuItem openItem = new MenuItem("Open...");
        openItem.setOnAction(e -> openFileDialog());
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> {
            saveCurrent();
        });
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> {
            saveAs();
        });

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
            dirty = false;
        }
        return root;
    }

    public void setMemoryUsageListener(IntConsumer listener) {
        this.memoryUsageListener = listener != null ? listener : usage -> {};
        reportMemoryUsage(textArea != null ? textArea.getText() : "");
    }

    private void openFileDialog() {
        if (!ensureChangesHandled()) {
            return;
        }
        VirtualDirectory startDir = currentFile != null ? currentFile.getParent() : fileSystem.getRootDirectory();
        if (startDir == null) {
            startDir = fileSystem.getRootDirectory();
        }
        VirtualFileChooser chooser = new VirtualFileChooser(fileSystem, startDir);
        Optional<VirtualFile> selection = chooser.showAndWait();
        selection.ifPresent(this::loadFile);
    }

    private void loadFile(VirtualFile file) {
        try {
            String content = fileSystem.readFile(file);
            currentFile = file;
            suppressTextEvents = true;
            textArea.setText(content);
            suppressTextEvents = false;
            statusText.setText("Opened " + file.getName());
            dirty = false;
            reportMemoryUsage(content);
        } catch (Exception e) {
            showError("Unable to read file");
        }
    }

    private boolean saveCurrent() {
        if (currentFile == null) {
            return saveAs();
        }
        try {
            fileSystem.writeFile(currentFile, textArea.getText());
            statusText.setText("Saved " + currentFile.getName());
            dirty = false;
            reportMemoryUsage(textArea.getText());
            return true;
        } catch (Exception e) {
            showError("Unable to save file");
            return false;
        }
    }

    private boolean saveAs() {
        TextInputDialog dialog = new TextInputDialog(currentFile != null ? currentFile.getName() : "newfile.txt");
        dialog.setTitle("Save As");
        dialog.setHeaderText("Enter a file name relative to the current directory");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return false;
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
                dirty = false;
                reportMemoryUsage(textArea.getText());
                return true;
            }
            return shouldWrite;
        } catch (Exception e) {
            showError("Unable to save file");
            return false;
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

    private boolean ensureChangesHandled() {
        if (!dirty) {
            return true;
        }
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        Alert prompt = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Save before continuing?",
                save, discard, ButtonType.CANCEL);
        prompt.setHeaderText(null);
        Optional<ButtonType> response = prompt.showAndWait();
        if (response.isEmpty() || response.get() == ButtonType.CANCEL) {
            return false;
        }
        if (response.get() == save) {
            return saveCurrent();
        }
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @Override
    public boolean requestClose() {
        return ensureChangesHandled();
    }
}
