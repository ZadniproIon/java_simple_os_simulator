package os.core;

import os.apps.OSApplication;

/**
 * Represents a lightweight OS process that wraps one in-app application instance.
 */
public class OSProcess {
    private final int pid;
    private final String name;
    private final int requiredMemory;
    private ProcessState state = ProcessState.NEW;
    private int priority = 1;
    private int allocatedMemory;
    private int cpuTimeUsed;
    private int ticksExecuted;
    private OSApplication application;

    public OSProcess(int pid, String name, int requiredMemory, OSApplication application) {
        this.pid = pid;
        this.name = name;
        this.requiredMemory = requiredMemory;
        this.application = application;
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

    public int getCpuTimeUsed() {
        return cpuTimeUsed;
    }

    public void incrementCpuTime() {
        this.cpuTimeUsed += 1;
    }

    public int getTicksExecuted() {
        return ticksExecuted;
    }

    public void incrementTicks() {
        this.ticksExecuted += 1;
    }

    public OSApplication getApplication() {
        return application;
    }

    public void setApplication(OSApplication application) {
        this.application = application;
    }
}
