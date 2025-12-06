package os.fs;

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
 * A sandboxed file system view rooted at a dedicated folder.
 */
public class VirtualFileSystem {
    private final Path root;

    public VirtualFileSystem(String baseDirectory) {
        this.root = Paths.get(baseDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create virtual file system root", e);
        }
    }

    public VirtualDirectory getRoot() {
        return new VirtualDirectory(root);
    }

    public List<VirtualFile> listFiles(VirtualDirectory directory) {
        if (!Files.isDirectory(directory.getPath())) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory.getPath())) {
            return stream
                    .map(path -> Files.isDirectory(path) ? null : new VirtualFile(path))
                    .filter(file -> file != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<VirtualDirectory> listDirectories(VirtualDirectory directory) {
        if (!Files.isDirectory(directory.getPath())) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory.getPath())) {
            return stream
                    .filter(Files::isDirectory)
                    .map(VirtualDirectory::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public VirtualFile createFile(VirtualDirectory directory, String name) {
        try {
            Path target = resolveInside(directory.getPath().resolve(name));
            Files.createDirectories(target.getParent());
            Files.createFile(target);
            return new VirtualFile(target);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create file", e);
        }
    }

    public VirtualDirectory createDirectory(VirtualDirectory parent, String name) {
        try {
            Path target = resolveInside(parent.getPath().resolve(name));
            Files.createDirectories(target);
            return new VirtualDirectory(target);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create directory", e);
        }
    }

    public void delete(VirtualFile file) {
        deletePath(file.getPath());
    }

    public void delete(VirtualDirectory directory) {
        deletePath(directory.getPath());
    }

    private void deletePath(Path path) {
        try {
            if (Files.notExists(path)) {
                return;
            }
            if (Files.isDirectory(path)) {
                try (Stream<Path> entries = Files.list(path)) {
                    entries.forEach(this::deletePath);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete path", e);
        }
    }

    public String readFile(VirtualFile file) {
        try {
            return Files.readString(file.getPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file", e);
        }
    }

    public void writeFile(VirtualFile file, String content) {
        try {
            Path path = file.getPath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file", e);
        }
    }

    public VirtualFile resolveFile(String relativePath) {
        Path path = resolveInside(root.resolve(relativePath));
        return new VirtualFile(path);
    }

    public VirtualDirectory resolveDirectory(String relativePath) {
        Path path = resolveInside(root.resolve(relativePath));
        return new VirtualDirectory(path);
    }

    private Path resolveInside(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalArgumentException("Path is outside of the virtual file system root");
        }
        return normalized;
    }
}
