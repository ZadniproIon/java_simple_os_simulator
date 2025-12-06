package os.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Very small memory manager that hands out coarse pages to processes.
 */
public class MemoryManager {
    private final int totalMemoryMb;
    private final int pageSizeMb;
    private final List<MemoryPage> pages = new ArrayList<>();

    public MemoryManager(int totalMemoryMb, int pageSizeMb) {
        this.totalMemoryMb = totalMemoryMb;
        this.pageSizeMb = pageSizeMb;
        int pageCount = totalMemoryMb / pageSizeMb;
        for (int i = 0; i < pageCount; i++) {
            pages.add(new MemoryPage(i, pageSizeMb));
        }
    }

    public List<MemoryPage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public int getTotalMemoryMb() {
        return totalMemoryMb;
    }

    public int getPageSizeMb() {
        return pageSizeMb;
    }

    public synchronized boolean allocateMemory(OSProcess process, int amountMb) {
        int pagesNeeded = (int) Math.ceil((double) amountMb / pageSizeMb);
        List<MemoryPage> freePages = new ArrayList<>();
        for (MemoryPage page : pages) {
            if (page.isFree()) {
                freePages.add(page);
            }
            if (freePages.size() >= pagesNeeded) {
                break;
            }
        }
        if (freePages.size() < pagesNeeded) {
            return false;
        }
        freePages.forEach(page -> page.setOwner(process));
        process.setAllocatedMemory(pagesNeeded * pageSizeMb);
        return true;
    }

    public synchronized void freeMemory(OSProcess process) {
        for (MemoryPage page : pages) {
            if (page.getOwner() == process) {
                page.setOwner(null);
            }
        }
        process.setAllocatedMemory(0);
    }
}
