package os.vfs;

/**
 * Represents a file node in the virtual file system.
 */
public class VirtualFile extends VirtualNode {

    public VirtualFile(String name, VirtualDirectory parent) {
        super(name, parent);
    }
}

