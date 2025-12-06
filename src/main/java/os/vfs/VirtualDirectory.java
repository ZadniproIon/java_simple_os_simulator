package os.vfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a directory node in the virtual file system.
 */
public class VirtualDirectory extends VirtualNode {

    // Optional in-memory cache; the VirtualFileSystem remains the source of truth.
    private final List<VirtualNode> children = new ArrayList<>();

    public VirtualDirectory(String name, VirtualDirectory parent) {
        super(name, parent);
    }

    public List<VirtualNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void setChildren(List<VirtualNode> newChildren) {
        children.clear();
        children.addAll(newChildren);
    }
}

