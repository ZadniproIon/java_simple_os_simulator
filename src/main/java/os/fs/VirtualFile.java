package os.fs;

import java.nio.file.Path;

/**
 * Wrapper around a host file used by the fake OS.
 */
public class VirtualFile {
    private final Path path;

    public VirtualFile(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return path.getFileName().toString();
    }
}
