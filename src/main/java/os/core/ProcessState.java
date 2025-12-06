package os.core;

/**
 * Represents the lifecycle states for a simulated OS process.
 */
public enum ProcessState {
    NEW,
    READY,
    RUNNING,
    WAITING,
    TERMINATED
}
