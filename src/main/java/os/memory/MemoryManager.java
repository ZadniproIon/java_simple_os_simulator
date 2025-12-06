package os.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import os.process.OSProcess;

/**
 * Very small paged memory manager. Uses a fixed-size pool of pages and
 * hands them out to OS processes on request.
 * <p>
 * All values are purely logical (e.g. "MB" is just a unit label). No
 * real memory allocation is performed.
 */
public class MemoryManager {

    private final int totalMemory; // in MB (conceptually)
    private final int pageSize;    // in MB (conceptually)
    private final List<MemoryPage> pages = new ArrayList<>();

    // Simple synthetic statistics for teaching purposes.
    private long totalAccesses;
    private long pageFaults;
    private long tlbHits;
    private long tlbMisses;

    public MemoryManager(int totalMemory, int pageSize) {
        if (totalMemory <= 0 || pageSize <= 0) {
            throw new IllegalArgumentException("Total memory and page size must be positive.");
        }
        if (totalMemory % pageSize != 0) {
            throw new IllegalArgumentException("Total memory must be a multiple of page size.");
        }
        this.totalMemory = totalMemory;
        this.pageSize = pageSize;
        int pageCount = totalMemory / pageSize;
        for (int i = 0; i < pageCount; i++) {
            pages.add(new MemoryPage(i));
        }
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<MemoryPage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    /**
     * Allocates memory for the given process. If enough free pages are available,
     * they are assigned to the process and its {@code allocatedMemory} field is
     * updated. Otherwise no pages are allocated.
     *
     * @return true if the allocation succeeded, false otherwise.
     */
    public synchronized boolean allocateMemory(OSProcess process, int amount) {
        int pagesNeeded = (int) Math.ceil((double) amount / pageSize);
        List<MemoryPage> freePages = new ArrayList<>();
        for (MemoryPage page : pages) {
            if (!page.isAllocated()) {
                freePages.add(page);
            }
            if (freePages.size() >= pagesNeeded) {
                break;
            }
        }
        if (freePages.size() < pagesNeeded) {
            return false;
        }
        for (MemoryPage page : freePages) {
            page.setAllocated(true);
            page.setOwner(process);
        }
        process.setAllocatedMemory(pagesNeeded * pageSize);
        return true;
    }

    /**
     * Frees all pages owned by the given process.
     */
    public synchronized void freeMemory(OSProcess process) {
        for (MemoryPage page : pages) {
            if (page.getOwner() == process) {
                page.setOwner(null);
                page.setAllocated(false);
            }
        }
        process.setAllocatedMemory(0);
    }

    public synchronized int getUsedMemory() {
        int usedPages = 0;
        for (MemoryPage page : pages) {
            if (page.isAllocated()) {
                usedPages++;
            }
        }
        return usedPages * pageSize;
    }

    public synchronized int getFreeMemory() {
        return totalMemory - getUsedMemory();
    }

    /**
     * Simulates a memory access by the given process. This does not model real
     * virtual memory, but it provides plausible counters for page faults and
     * TLB behaviour that can be visualised in the GUI.
     */
    public synchronized void simulateAccess(OSProcess process) {
        if (process == null) {
            return;
        }
        totalAccesses++;

        // Basic heuristic: processes with more allocated memory tend to have a
        // better TLB hit rate.
        double memoryFraction = (double) process.getAllocatedMemory() / Math.max(totalMemory, 1);
        double hitProbability = 0.4 + Math.min(memoryFraction, 0.5); // between 0.4 and 0.9

        if (Math.random() < hitProbability) {
            tlbHits++;
            return;
        }

        tlbMisses++;

        // On a TLB miss, occasionally treat it as a page fault.
        if (Math.random() < 0.3) {
            pageFaults++;
        }
    }

    public synchronized long getTotalAccesses() {
        return totalAccesses;
    }

    public synchronized long getPageFaults() {
        return pageFaults;
    }

    public synchronized long getTlbHits() {
        return tlbHits;
    }

    public synchronized long getTlbMisses() {
        return tlbMisses;
    }
}
