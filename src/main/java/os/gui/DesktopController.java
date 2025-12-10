package os.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import os.apps.FileExplorerApp;
import os.apps.NotepadApp;
import os.apps.OSApplication;
import os.apps.SettingsApp;
import os.apps.SystemMonitorApp;
import os.apps.TaskManagerApp;
import os.gui.wallpaper.WallpaperService;
import os.process.OSKernel;
import os.process.OSProcess;
import os.process.ProcessListener;
import os.users.UserAccount;
import os.users.UserRole;
import os.vfs.VirtualDirectory;
import os.vfs.VirtualFile;

/**
 * Builds the desktop scene, including icons, taskbar and app windows.
 */
public class DesktopController implements ProcessListener {
    private final OSKernel kernel;
    private final Runnable logoutHandler;
    private final BorderPane root = new BorderPane();
    // Desktop area where icons and internal windows live.
    private final Pane desktopArea = new Pane();
    // Layer used to host overlays (e.g., app drawer) on top of the desktop.
    private final StackPane centerLayer = new StackPane();
    private final TaskbarController taskbar;
    private final Map<Integer, OSWindow> openWindows = new HashMap<>();
    private final Map<Integer, OSApplication> applications = new HashMap<>();
    private final FlowPane iconPane = new FlowPane(10, 10);
    private StackPane appDrawerOverlay;
    private final WallpaperService wallpaperService;

    public DesktopController(OSKernel kernel, Runnable logoutHandler, WallpaperService wallpaperService) {
        this.kernel = kernel;
        this.logoutHandler = logoutHandler;
        this.wallpaperService = wallpaperService;
        this.kernel.addProcessListener(this);
        this.taskbar = new TaskbarController(this::openAppDrawer);
        setupLayout();
        setupIcons();
        refreshWallpaper();
    }

    private void setupLayout() {
        desktopArea.getStyleClass().add("desktop-root");
        desktopArea.setPrefSize(1000, 700);
        iconPane.setPadding(new Insets(15));
        iconPane.getStyleClass().add("desktop-icon-grid");
        desktopArea.getChildren().add(iconPane);
        centerLayer.getChildren().add(desktopArea);
        root.setCenter(centerLayer);
        root.setBottom(taskbar.getView());
    }

    private void setupIcons() {
        iconPane.getChildren().add(createIcon("Notepad", () ->
                launchApplication("Notepad", () -> new NotepadApp(kernel.getFileSystem()), 48, 32, 96)));

        iconPane.getChildren().add(createIcon("File Explorer", () -> {
            UserAccount current = kernel.getAuthManager().getCurrentUser();
            boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
            VirtualDirectory rootDir = isAdmin
                    ? kernel.getFileSystem().getRootDirectory()
                    : kernel.getCurrentUserHomeDirectory();
            launchApplication("File Explorer",
                    () -> new FileExplorerApp(kernel.getFileSystem(), rootDir, current, this::openFileInNotepad),
                    96, 64, 160);
        }));
        iconPane.getChildren().add(createIcon("Task Manager", () ->
                launchApplication("Task Manager", () -> new TaskManagerApp(kernel), 128, 96, 192)));
        iconPane.getChildren().add(createIcon("System Monitor", () ->
                launchApplication("System Monitor", () -> new SystemMonitorApp(kernel), 256, 180, 320)));
        iconPane.getChildren().add(createIcon("Settings", () ->
                launchApplication("Settings",
                        () -> new SettingsApp(kernel, logoutHandler, wallpaperService, this::refreshWallpaper),
                        64, 48, 120)));
    }

    private Node createIcon(String title, Runnable action) {
        Button button = new Button(title);
        button.setPrefSize(120, 80);
        button.setWrapText(true);
        button.getStyleClass().add("desktop-icon");
        button.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                action.run();
            }
        });
        return button;
    }

    private void launchApplication(String processName, Supplier<OSApplication> factory, int memoryRequired) {
        launchApplication(processName, factory, memoryRequired,
                Math.max(64, memoryRequired - 64), memoryRequired + 96);
    }

    private void launchApplication(String processName,
                                   Supplier<OSApplication> factory,
                                   int memoryRequired,
                                   int minSimulatedUsage,
                                   int maxSimulatedUsage) {
        OSApplication app = factory.get();
        OSProcess process = kernel.createProcess(processName, memoryRequired);
        if (process == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Not enough memory to start " + processName,
                    ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        process.configureMemoryProfile(minSimulatedUsage, maxSimulatedUsage, memoryRequired);
        if (app instanceof NotepadApp notepad) {
            notepad.setMemoryUsageListener(usage ->
                    process.setSimulatedMemoryUsage(Math.max(minSimulatedUsage, Math.min(maxSimulatedUsage, usage))));
        }

        app.onStart();
        applications.put(process.getPid(), app);

        OSWindow window = new OSWindow(app.getName() + " (PID " + process.getPid() + ")",
                app.createContent());
        window.setLayoutX(100 + openWindows.size() * 20);
        window.setLayoutY(80 + openWindows.size() * 20);
        window.setOnCloseRequest(() -> {
            app.onStop();
            kernel.killProcess(process.getPid());
        });
        desktopArea.getChildren().add(window);
        openWindows.put(process.getPid(), window);
        taskbar.addProcess(process, () -> focusWindow(process.getPid()));
    }

    private void focusWindow(int pid) {
        OSWindow window = openWindows.get(pid);
        if (window != null) {
            window.restore();
            window.toFront();
        }
    }

    private void openFileInNotepad(VirtualFile file) {
        launchApplication("Notepad", () -> new NotepadApp(kernel.getFileSystem(), file), 48, 32, 96);
    }

    /**
     * Opens a modal-style app drawer overlay centered on the desktop.
     * Clicking outside the drawer will close it.
     */
    private void openAppDrawer() {
        if (appDrawerOverlay != null && centerLayer.getChildren().contains(appDrawerOverlay)) {
            appDrawerOverlay.toFront();
            return;
        }

        javafx.scene.layout.VBox list = new javafx.scene.layout.VBox(8);
        list.setPadding(new Insets(15));
        list.setPrefSize(400, 400);
        list.setMaxSize(400, 400);
        list.getStyleClass().add("app-drawer-panel");

        list.getChildren().add(createDrawerButton("Notepad",
                () -> launchApplication("Notepad", () -> new NotepadApp(kernel.getFileSystem()), 48, 32, 96)));

        list.getChildren().add(createDrawerButton("File Explorer",
                () -> {
                    UserAccount current = kernel.getAuthManager().getCurrentUser();
                    boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
                    VirtualDirectory rootDir = isAdmin
                            ? kernel.getFileSystem().getRootDirectory()
                            : kernel.getCurrentUserHomeDirectory();
                    launchApplication("File Explorer",
                            () -> new FileExplorerApp(kernel.getFileSystem(), rootDir, current, this::openFileInNotepad),
                            96, 64, 160);
                }));

        list.getChildren().add(createDrawerButton("Task Manager",
                () -> launchApplication("Task Manager", () -> new TaskManagerApp(kernel), 128, 96, 192)));

        list.getChildren().add(createDrawerButton("System Monitor",
                () -> launchApplication("System Monitor", () -> new SystemMonitorApp(kernel), 256, 180, 320)));

        list.getChildren().add(createDrawerButton("Settings",
                () -> launchApplication("Settings",
                        () -> new SettingsApp(kernel, logoutHandler, wallpaperService, this::refreshWallpaper),
                        64, 48, 120)));

        appDrawerOverlay = new StackPane();
        appDrawerOverlay.getStyleClass().add("app-drawer");
        appDrawerOverlay.getChildren().add(list);
        StackPane.setAlignment(list, javafx.geometry.Pos.CENTER);

        // Close when clicking outside the drawer content.
        appDrawerOverlay.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!list.localToScene(list.getBoundsInLocal()).contains(event.getSceneX(), event.getSceneY())) {
                closeAppDrawerOverlay();
            }
        });

        centerLayer.getChildren().add(appDrawerOverlay);
        appDrawerOverlay.toFront();
    }

    private Button createDrawerButton(String label, Runnable action) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> {
            // Close drawer first so new app window appears cleanly on the desktop.
            closeAppDrawerOverlay();
            action.run();
        });
        button.getStyleClass().add("drawer-button");
        return button;
    }

    private void closeAppDrawerOverlay() {
        if (appDrawerOverlay != null && centerLayer.getChildren().contains(appDrawerOverlay)) {
            centerLayer.getChildren().remove(appDrawerOverlay);
        }
        appDrawerOverlay = null;
    }

    /**
     * Re-applies the wallpaper based on the currently authenticated user.
     * Call this after login/logout or when a user changes their preference.
     */
    public void refreshWallpaper() {
        UserAccount current = kernel.getAuthManager().getCurrentUser();
        wallpaperService.applyWallpaper(desktopArea, current);
    }

    public Parent getView() {
        return root;
    }

    @Override
    public void processTerminated(OSProcess process) {
        Platform.runLater(() -> {
            OSApplication app = applications.remove(process.getPid());
            if (app != null) {
                app.onStop();
            }
            closeWindow(process.getPid());
        });
    }

    private void closeWindow(int pid) {
        OSWindow window = openWindows.remove(pid);
        if (window != null) {
            desktopArea.getChildren().remove(window);
            taskbar.removeProcess(pid);
        }
    }
}
