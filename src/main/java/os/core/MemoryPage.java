package os.core;

/**
 * Represents a coarse memory page owned by a process.
 */
public class MemoryPage {
    private final int pageNumber;
    private final int sizeMb;
    private OSProcess owner;

    public MemoryPage(int pageNumber, int sizeMb) {
        this.pageNumber = pageNumber;
        this.sizeMb = sizeMb;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getSizeMb() {
        return sizeMb;
    }

    public OSProcess getOwner() {
        return owner;
    }

    public void setOwner(OSProcess owner) {
        this.owner = owner;
    }

    public boolean isFree() {
        return owner == null;
    }
}
