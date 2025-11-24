package os.core;

/**
 * Listener for process lifecycle changes so the GUI can react.
 */
public interface ProcessLifecycleListener {
    default void processCreated(OSProcess process) {}

    default void processTerminated(OSProcess process) {}
}
