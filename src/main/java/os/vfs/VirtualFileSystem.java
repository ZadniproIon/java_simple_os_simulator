package os.vfs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Very small virtual file system that wraps the local host file system
 * under a dedicated root directory.
 * <p>
 * All paths are resolved relative to the configured root, so applications
 * never receive raw host paths. This makes it easy to sandbox the fake OS.
 */
public class VirtualFileSystem {

    private final Path rootPath;
    private final VirtualDirectory rootDirectory;

    public VirtualFileSystem(String rootFolderName) {
        this.rootPath = Paths.get(rootFolderName).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create virtual FS root: " + rootPath, e);
        }
        // Root has no parent and an empty name; getPath() will render it as "/".
        this.rootDirectory = new VirtualDirectory("", null);
        ensureStandardLayout();
    }

    public VirtualDirectory getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Lists the children of the given directory by reading from the host file system.
     */
    public List<VirtualNode> list(VirtualDirectory dir) {
        Path realPath = toRealPath(dir);
        if (!Files.isDirectory(realPath)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(realPath)) {
            List<VirtualNode> nodes = stream
                    .map(path -> {
                        String name = path.getFileName().toString();
                        if (Files.isDirectory(path)) {
                            return new VirtualDirectory(name, dir);
                        } else {
                            return new VirtualFile(name, dir);
                        }
                    })
                    .collect(Collectors.toList());
            dir.setChildren(nodes);
            return nodes;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public VirtualDirectory createDirectory(VirtualDirectory parent, String name) {
        Path realPath = toRealPath(parent).resolve(name);
        try {
            Files.createDirectories(realPath);
            VirtualDirectory directory = new VirtualDirectory(name, parent);
            // Optionally update parent's cached children.
            list(parent);
            return directory;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create directory " + realPath, e);
        }
    }

    public VirtualFile createFile(VirtualDirectory parent, String name) {
        Path realPath = toRealPath(parent).resolve(name);
        try {
            if (realPath.getParent() != null) {
                Files.createDirectories(realPath.getParent());
            }
            Files.createFile(realPath);
            VirtualFile file = new VirtualFile(name, parent);
            list(parent);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create file " + realPath, e);
        }
    }

    public void delete(VirtualNode node) {
        Path realPath = toRealPath(node);
        try {
            if (Files.notExists(realPath)) {
                return;
            }
            if (Files.isDirectory(realPath)) {
                try (Stream<Path> stream = Files.walk(realPath)) {
                    stream.sorted((a, b) -> b.compareTo(a)) // delete children before parent
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // best-effort delete; fine for a teaching example
                                }
                            });
                }
            } else {
                Files.deleteIfExists(realPath);
            }
            // Refresh parent's children cache if possible.
            if (node.getParent() != null) {
                list(node.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete " + realPath, e);
        }
    }

    public String readFile(VirtualFile file) {
        Path realPath = toRealPath(file);
        try {
            return Files.readString(realPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file " + realPath, e);
        }
    }

    public void writeFile(VirtualFile file, String content) {
        Path realPath = toRealPath(file);
        try {
            if (realPath.getParent() != null) {
                Files.createDirectories(realPath.getParent());
            }
            Files.writeString(realPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file " + realPath, e);
        }
    }

    /**
     * Returns true if the given virtual file currently exists on disk.
     */
    public boolean exists(VirtualFile file) {
        Path realPath = toRealPath(file);
        return Files.exists(realPath);
    }

    /**
     * Resolves a virtual file path (e.g. "docs/readme.txt" or "/docs/readme.txt")
     * into a {@link VirtualFile} object. The file is not created or validated on
     * disk; callers can decide whether to create or read it.
     */
    public VirtualFile resolveFile(String virtualPath) {
        if (virtualPath == null || virtualPath.isBlank() || "/".equals(virtualPath)) {
            throw new IllegalArgumentException("File path must not be empty or root.");
        }
        String normalized = virtualPath.startsWith("/") ? virtualPath.substring(1) : virtualPath;
        int lastSlash = normalized.lastIndexOf('/');
        String dirPart = lastSlash >= 0 ? normalized.substring(0, lastSlash) : "";
        String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        VirtualDirectory parentDir = resolveDirectory(dirPart);
        return new VirtualFile(fileName, parentDir);
    }

    /**
     * Resolves a virtual directory path (e.g. "docs" or "/docs") into a
     * {@link VirtualDirectory} instance. The directory is not created on disk
     * automatically; callers can use {@link #createDirectory(VirtualDirectory, String)}
     * if they need to ensure it exists.
     */
    public VirtualDirectory resolveDirectory(String virtualPath) {
        if (virtualPath == null || virtualPath.isBlank() || "/".equals(virtualPath)) {
            return rootDirectory;
        }
        String normalized = virtualPath.startsWith("/") ? virtualPath.substring(1) : virtualPath;
        String[] segments = normalized.split("/");
        VirtualDirectory current = rootDirectory;
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            current = new VirtualDirectory(segment, current);
        }
        return current;
    }

    private Path toRealPath(VirtualNode node) {
        String virtualPath = node.getPath(); // e.g. "/", "/docs", "/docs/file.txt"
        if ("/".equals(virtualPath)) {
            return rootPath;
        }
        String relative = virtualPath.startsWith("/") ? virtualPath.substring(1) : virtualPath;
        return rootPath.resolve(relative).normalize();
    }

    /**
     * Exposes the host file system path corresponding to a virtual node.
     * Useful for components that need to stream binary data such as wallpaper images.
     */
    public Path toHostPath(VirtualNode node) {
        return toRealPath(node);
    }

    /**
     * Ensures that the given virtual directory exists on disk and returns it.
     */
    public VirtualDirectory ensureDirectory(String virtualPath) {
        VirtualDirectory directory = resolveDirectory(virtualPath);
        Path real = toRealPath(directory);
        try {
            Files.createDirectories(real);
        } catch (IOException ignored) {
        }
        return directory;
    }

    /**
     * Ensures that standard directories such as /home, /bin, /etc and /tmp
     * exist under the virtual root.
     */
    private void ensureStandardLayout() {
        String[] standardDirs = { "home", "bin", "etc", "tmp", "system" };
        for (String dir : standardDirs) {
            Path path = rootPath.resolve(dir);
            try {
                Files.createDirectories(path);
            } catch (IOException ignored) {
                // Best-effort; failures are not fatal for the simulator.
            }
        }
    }

    /**
     * Returns a user's home directory under /home, creating it if necessary.
     */
    public VirtualDirectory getHomeDirectory(String username) {
        VirtualDirectory homeRoot = resolveDirectory("/home");
        VirtualDirectory userHome = new VirtualDirectory(username, homeRoot);
        Path real = toRealPath(userHome);
        try {
            Files.createDirectories(real);
        } catch (IOException ignored) {
        }
        return userHome;
    }
}
