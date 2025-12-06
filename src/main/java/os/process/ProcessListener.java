package os.process;

/**
 * Listener for process lifecycle events emitted by {@link OSKernel}.
 * <p>
 * This allows higher layers (GUI, logging, etc.) to react when
 * processes are created or terminated without coupling them tightly
 * to the kernel implementation.
 */
public interface ProcessListener {

    default void processCreated(OSProcess process) {
        // default no-op
    }

    default void processTerminated(OSProcess process) {
        // default no-op
    }
}

