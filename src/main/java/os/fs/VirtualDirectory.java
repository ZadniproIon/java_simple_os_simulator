package os.fs;

import java.nio.file.Path;

/**
 * Wrapper for directories within the VirtualFileSystem root.
 */
public class VirtualDirectory {
    private final Path path;

    public VirtualDirectory(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return path.getFileName() != null ? path.getFileName().toString() : path.toString();
    }
}
