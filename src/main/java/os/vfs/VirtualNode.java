package os.vfs;

/**
 * Base type for all nodes in the virtual file system tree.
 * <p>
 * Nodes are arranged in a simple directory hierarchy rooted at the
 * {@link VirtualDirectory} returned by the {@code VirtualFileSystem}.
 */
public abstract class VirtualNode {

    private final String name;
    private final VirtualDirectory parent;

    protected VirtualNode(String name, VirtualDirectory parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public VirtualDirectory getParent() {
        return parent;
    }

    /**
     * Returns the virtual path of this node, starting at the root directory.
     * Paths use Unix-like semantics (e.g. "/docs/readme.txt").
     */
    public String getPath() {
        if (parent == null) {
            return "/"; // root
        }
        String parentPath = parent.getPath();
        if ("/".equals(parentPath)) {
            return parentPath + name;
        }
        return parentPath + "/" + name;
    }
}

