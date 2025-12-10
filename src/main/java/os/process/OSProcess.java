package os.process;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a single simulated process in the fake OS.
 * <p>
 * No real threads or OS processes are created; instead, we track
 * state and accounting information such as required memory and
 * simulated CPU time.
 */
public class OSProcess {

    private final int pid;
    private final String name;

    private ProcessState state = ProcessState.NEW;
    private int priority = 1;
    private final int requiredMemory;
    private int allocatedMemory;
    private long cpuTimeUsed;
    private int estimatedBurstTime = 10;

    private int minSimulatedMemory;
    private int maxSimulatedMemory;
    private int simulatedMemoryUsage;

    public OSProcess(int pid, String name, int requiredMemory) {
        this.pid = pid;
        this.name = Objects.requireNonNull(name, "name");
        this.requiredMemory = requiredMemory;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getRequiredMemory() {
        return requiredMemory;
    }

    public int getAllocatedMemory() {
        return allocatedMemory;
    }

    public void setAllocatedMemory(int allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }

    public long getCpuTimeUsed() {
        return cpuTimeUsed;
    }

    /**
     * Simulates the consumption of one CPU tick by this process.
     */
    public void incrementCpuTime() {
        this.cpuTimeUsed++;
    }

    /**
     * Estimated total CPU burst length (in ticks) for SJF/priority decisions.
     */
    public int getEstimatedBurstTime() {
        return estimatedBurstTime;
    }

    public void setEstimatedBurstTime(int estimatedBurstTime) {
        this.estimatedBurstTime = Math.max(1, estimatedBurstTime);
    }

    public void configureMemoryProfile(int minMb, int maxMb, int initialMb) {
        this.minSimulatedMemory = Math.max(0, minMb);
        this.maxSimulatedMemory = Math.max(this.minSimulatedMemory + 1, maxMb);
        this.simulatedMemoryUsage = clamp(initialMb, this.minSimulatedMemory, this.maxSimulatedMemory);
    }

    public int getSimulatedMemoryUsage() {
        if (simulatedMemoryUsage <= 0) {
            return allocatedMemory > 0 ? allocatedMemory : requiredMemory;
        }
        return simulatedMemoryUsage;
    }

    public void fluctuateMemoryUsage() {
        if (maxSimulatedMemory <= minSimulatedMemory) {
            return;
        }
        int delta = ThreadLocalRandom.current().nextInt(-15, 16);
        simulatedMemoryUsage = clamp(simulatedMemoryUsage + delta, minSimulatedMemory, maxSimulatedMemory);
    }

    public void setSimulatedMemoryUsage(int usageMb) {
        if (usageMb <= 0) {
            return;
        }
        simulatedMemoryUsage = clamp(usageMb, minSimulatedMemory, maxSimulatedMemory);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
