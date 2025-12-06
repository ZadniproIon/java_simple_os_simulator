package os.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import os.apps.OSApplication;
import os.fs.VirtualFileSystem;

/**
 * Coordinates the simulated OS subsystems such as processes, memory and the scheduler.
 */
public class OSKernel {
    private final ObservableList<OSProcess> processes = FXCollections.observableArrayList();
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    private final VirtualFileSystem fileSystem;
    private final AuthManager authManager;
    private final AtomicInteger pidGenerator = new AtomicInteger(100);
    private final List<ProcessLifecycleListener> listeners = new ArrayList<>();

    private OSProcess currentProcess;
    private Timeline cpuClock;

    public OSKernel(Scheduler scheduler, MemoryManager memoryManager, VirtualFileSystem fileSystem, AuthManager authManager) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
        this.fileSystem = fileSystem;
        this.authManager = authManager;
    }

    public synchronized Optional<OSProcess> createProcess(String name, OSApplication application, int memoryRequired) {
        OSProcess process = new OSProcess(pidGenerator.getAndIncrement(), name, memoryRequired, application);
        process.setState(ProcessState.NEW);
        if (!memoryManager.allocateMemory(process, memoryRequired)) {
            process.setState(ProcessState.TERMINATED);
            return Optional.empty();
        }
        process.setState(ProcessState.READY);
        processes.add(process);
        scheduler.addProcess(process);
        application.onStart();
        listeners.forEach(listener -> listener.processCreated(process));
        return Optional.of(process);
    }

    public synchronized boolean killProcess(int pid) {
        Optional<OSProcess> optional = findProcess(pid);
        if (optional.isEmpty()) {
            return false;
        }
        OSProcess process = optional.get();
        process.setState(ProcessState.TERMINATED);
        scheduler.removeProcess(process);
        if (currentProcess == process) {
            currentProcess = null;
        }
        memoryManager.freeMemory(process);
        OSApplication application = process.getApplication();
        if (application != null) {
            application.onStop();
        }
        processes.remove(process);
        listeners.forEach(listener -> listener.processTerminated(process));
        return true;
    }

    public synchronized Optional<OSProcess> findProcess(int pid) {
        return processes.stream().filter(process -> process.getPid() == pid).findFirst();
    }

    public synchronized void tick() {
        if (currentProcess != null) {
            currentProcess.incrementCpuTime();
            currentProcess.incrementTicks();
            if (scheduler.getAlgorithm() == SchedulingAlgorithm.ROUND_ROBIN) {
                if (currentProcess.getState() != ProcessState.TERMINATED) {
                    currentProcess.setState(ProcessState.READY);
                    scheduler.requeue(currentProcess);
                }
                currentProcess = null;
            } else if (currentProcess.getState() == ProcessState.TERMINATED) {
                currentProcess = null;
            } else {
                // FCFS keeps running until termination, so nothing else to do for this tick.
                return;
            }
        }
        OSProcess next = scheduler.scheduleNext();
        if (next != null && next.getState() != ProcessState.TERMINATED) {
            next.setState(ProcessState.RUNNING);
            currentProcess = next;
        }
    }

    public synchronized void startScheduler() {
        if (cpuClock != null) {
            cpuClock.stop();
        }
        cpuClock = new Timeline(new KeyFrame(Duration.millis(500), event -> tick()));
        cpuClock.setCycleCount(Timeline.INDEFINITE);
        cpuClock.play();
    }

    public synchronized void shutdown() {
        if (cpuClock != null) {
            cpuClock.stop();
        }
        List<OSProcess> snapshot = List.copyOf(processes);
        snapshot.forEach(process -> killProcess(process.getPid()));
    }

    public ObservableList<OSProcess> getProcesses() {
        return FXCollections.unmodifiableObservableList(processes);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public void addProcessLifecycleListener(ProcessLifecycleListener listener) {
        listeners.add(listener);
    }
}
