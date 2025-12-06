package os.memory;

import os.process.OSProcess;

/**
 * Represents a coarse-grained page of simulated physical memory.
 * <p>
 * Each page may be either free or owned by exactly one {@link OSProcess}.
 */
public class MemoryPage {

    private final int pageNumber;
    private OSProcess owner;
    private boolean allocated;

    public MemoryPage(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public OSProcess getOwner() {
        return owner;
    }

    public void setOwner(OSProcess owner) {
        this.owner = owner;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }
}

