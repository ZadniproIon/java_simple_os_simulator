package os.gui.wallpaper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import os.users.UserAccount;
import os.vfs.VirtualDirectory;
import os.vfs.VirtualFileSystem;

/**
 * Handles loading wallpaper assets into the virtual file system and
 * provides helpers for applying them to JavaFX panes.
 */
public class WallpaperService {

    private static final String WALLPAPER_DIR = "/system/wallpapers";
    private final VirtualFileSystem fileSystem;
    private final VirtualDirectory wallpaperDirectory;
    private final Path wallpaperStorage;
    private final List<String> wallpaperNames = new ArrayList<>();

    public WallpaperService(VirtualFileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.wallpaperDirectory = fileSystem.ensureDirectory(WALLPAPER_DIR);
        this.wallpaperStorage = fileSystem.toHostPath(wallpaperDirectory);
        seedFromLegacyFolder(Paths.get("wallpapers"));
        reloadWallpaperList();
    }

    /**
     * Copies wallpapers from the legacy project folder into the virtual FS.
     * This allows us to treat wallpapers like any other system asset.
     */
    private void seedFromLegacyFolder(Path legacyFolder) {
        if (!Files.isDirectory(legacyFolder)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(legacyFolder)) {
            for (Path source : stream) {
                if (!Files.isRegularFile(source)) {
                    continue;
                }
                String lower = source.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
                    continue;
                }
                Path target = wallpaperStorage.resolve(source.getFileName().toString());
                if (Files.exists(target)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            }
        } catch (IOException ignored) {
            // Best effort: if wallpapers fail to copy we simply rely on existing files.
        }
    }

    private void reloadWallpaperList() {
        wallpaperNames.clear();
        if (!Files.isDirectory(wallpaperStorage)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(wallpaperStorage)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    wallpaperNames.add(path.getFileName().toString());
                }
            }
        } catch (IOException ignored) {
        }
        wallpaperNames.sort(String::compareToIgnoreCase);
    }

    public List<String> getAvailableWallpapers() {
        return Collections.unmodifiableList(wallpaperNames);
    }

    public String getDefaultWallpaper() {
        if (wallpaperNames.contains(UserAccount.DEFAULT_WALLPAPER)) {
            return UserAccount.DEFAULT_WALLPAPER;
        }
        return wallpaperNames.isEmpty() ? null : wallpaperNames.get(0);
    }

    public String resolveWallpaperName(String requestedName) {
        if (requestedName != null) {
            for (String candidate : wallpaperNames) {
                if (candidate.equalsIgnoreCase(requestedName)) {
                    return candidate;
                }
            }
        }
        return getDefaultWallpaper();
    }

    /**
     * Applies the wallpaper associated with the user (or default) to the given pane.
     */
    public void applyWallpaper(Pane target, UserAccount user) {
        if (target == null) {
            return;
        }
        String wallpaperName = user != null ? resolveWallpaperName(user.getPreferredWallpaper())
                : getDefaultWallpaper();
        applyWallpaper(target, wallpaperName);
    }

    public void applyWallpaper(Pane target, String wallpaperName) {
        if (target == null) {
            return;
        }
        Background background = buildBackground(wallpaperName);
        if (background != null) {
            target.setBackground(background);
            target.setStyle("");
        } else {
            target.setBackground(null);
            target.setStyle("-fx-background-color: linear-gradient(#1b2838, #0f2027);");
        }
    }

    public Background buildBackground(String wallpaperName) {
        String resolved = resolveWallpaperName(wallpaperName);
        if (resolved == null) {
            return null;
        }
        Path path = wallpaperStorage.resolve(resolved);
        if (!Files.exists(path)) {
            return null;
        }
        Image image = new Image(path.toUri().toString(), 0, 0, true, true);
        BackgroundSize size = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO,
                false, false, true, true);
        BackgroundImage bgImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size);
        return new Background(bgImage);
    }

    public Image loadPreview(String wallpaperName, double width, double height) {
        String resolved = resolveWallpaperName(wallpaperName);
        if (resolved == null) {
            return null;
        }
        Path path = wallpaperStorage.resolve(resolved);
        if (!Files.exists(path)) {
            return null;
        }
        return new Image(path.toUri().toString(), width, height, true, true);
    }

    public record WallpaperOption(String fileName, String displayName) {}

    public List<WallpaperOption> getWallpaperOptions() {
        List<WallpaperOption> options = new ArrayList<>();
        for (String fileName : wallpaperNames) {
            String display = buildDisplayName(fileName);
            options.add(new WallpaperOption(fileName, display));
        }
        return options;
    }

    private String buildDisplayName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        base = base.replace('-', ' ').replace('_', ' ');
        if (base.isEmpty()) {
            return fileName;
        }
        return base.substring(0, 1).toUpperCase(Locale.ROOT) + base.substring(1);
    }
}
