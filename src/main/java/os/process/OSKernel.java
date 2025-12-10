package os.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import os.memory.MemoryManager;
import os.users.AuthManager;
import os.users.UserAccount;
import os.vfs.VirtualDirectory;
import os.vfs.VirtualFileSystem;

/**
 * Central coordinator for the simulation back-end.
 * <p>
 * The kernel knows about the process table, scheduler, memory manager,
 * authentication system and virtual file system. A GUI, CLI or test
 * harness can drive the system by repeatedly calling {@link #tick()}.
 */
public class OSKernel {

    private final List<OSProcess> processes = new ArrayList<>();
    private final List<ProcessListener> listeners = new ArrayList<>();
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    private final AuthManager authManager;
    private final VirtualFileSystem fileSystem;

    // Scheduling history for visualisation (PID per tick, 0 = idle).
    private final List<Integer> runHistory = new ArrayList<>();
    private long tickCounter;

    private int nextPid = 1;

    public OSKernel(MemoryManager memoryManager,
                    Scheduler scheduler,
                    AuthManager authManager,
                    VirtualFileSystem fileSystem) {
        this.memoryManager = Objects.requireNonNull(memoryManager, "memoryManager");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.authManager = Objects.requireNonNull(authManager, "authManager");
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem");
    }

    /**
     * Convenience constructor that creates a default FCFS scheduler.
     */
    public OSKernel(MemoryManager memoryManager,
                    AuthManager authManager,
                    VirtualFileSystem fileSystem) {
        this(memoryManager, new Scheduler(SchedulingAlgorithm.FCFS), authManager, fileSystem);
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * Creates a new process and attempts to allocate memory for it.
     *
     * @return the created process, or {@code null} if memory allocation failed.
     */
    public synchronized OSProcess createProcess(String name, int requiredMemory) {
        OSProcess process = new OSProcess(nextPid++, name, requiredMemory);
        process.setState(ProcessState.NEW);
        // Use a simple heuristic for burst: proportional to required memory.
        process.setEstimatedBurstTime(Math.max(5, requiredMemory / 16));

        boolean allocated = memoryManager.allocateMemory(process, requiredMemory);
        if (!allocated) {
            // Not enough simulated memory; process never becomes READY.
            process.setState(ProcessState.TERMINATED);
            return null;
        }

        process.setState(ProcessState.READY);
        processes.add(process);
        scheduler.addProcess(process);
        // Notify observers that a new process entered the system.
        for (ProcessListener listener : listeners) {
            listener.processCreated(process);
        }
        return process;
    }

    /**
     * Terminates a process, frees its memory and removes it from the scheduler.
     */
    public synchronized void killProcess(int pid) {
        OSProcess process = findProcess(pid);
        if (process == null) {
            return;
        }
        process.setState(ProcessState.TERMINATED);
        memoryManager.freeMemory(process);
        scheduler.removeProcess(process);
        processes.remove(process);
        for (ProcessListener listener : listeners) {
            listener.processTerminated(process);
        }
    }

    public synchronized OSProcess findProcess(int pid) {
        for (OSProcess process : processes) {
            if (process.getPid() == pid) {
                return process;
            }
        }
        return null;
    }

    public synchronized List<OSProcess> getProcesses() {
        return Collections.unmodifiableList(processes);
    }

    public synchronized void addProcessListener(ProcessListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Ensures that the currently authenticated user has a home directory
     * under /home/&lt;user&gt; in the virtual file system.
     */
    public synchronized VirtualDirectory ensureCurrentUserHomeDirectory() {
        UserAccount user = authManager.getCurrentUser();
        if (user == null) {
            return fileSystem.getRootDirectory();
        }
        return fileSystem.getHomeDirectory(user.getUsername());
    }

    /**
     * Returns a snapshot of the scheduling history as a list of PIDs, where
     * each entry corresponds to one call to {@link #tick()}. A value of 0
     * represents an idle tick with no running process.
     */
    public synchronized List<Integer> getRunHistory() {
        return new ArrayList<>(runHistory);
    }

    /**
     * Returns the current user's home directory if logged in, otherwise the root.
     */
    public synchronized VirtualDirectory getCurrentUserHomeDirectory() {
        UserAccount user = authManager.getCurrentUser();
        if (user == null) {
            return fileSystem.getRootDirectory();
        }
        return fileSystem.getHomeDirectory(user.getUsername());
    }

    /**
     * Simulates one CPU time slice.
     * <ol>
     *     <li>The scheduler chooses a process.</li>
     *     <li>The process is marked RUNNING and its CPU time incremented.</li>
     *     <li>The process is placed back into READY state (or stays TERMINATED).</li>
     * </ol>
     * A higher-level component (e.g. GUI timer) should call this method
     * periodically to advance the simulation.
     */
    public synchronized void tick() {
        tickCounter++;
        OSProcess process = scheduler.getNextProcess();
        if (process == null || process.getState() == ProcessState.TERMINATED) {
            runHistory.add(0);
            return;
        }

        if (process.getState() == ProcessState.NEW) {
            process.setState(ProcessState.READY);
        }

        // Simulate running for one time slice.
        process.setState(ProcessState.RUNNING);
        process.incrementCpuTime();
        // Synthetic memory access used to drive page fault / TLB statistics.
        memoryManager.simulateAccess(process);
        process.fluctuateMemoryUsage();

        // For this basic model we simply move the process back to READY so that
        // the scheduler can choose it again in a later tick.
        if (process.getState() == ProcessState.RUNNING) {
            process.setState(ProcessState.READY);
        }

        runHistory.add(process.getPid());

        // Keep history bounded so the UI does not grow without limit.
        if (runHistory.size() > 500) {
            runHistory.remove(0);
        }
    }
}
