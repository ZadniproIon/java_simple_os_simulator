package os.process;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

/**
 * Simple scheduler that chooses which READY process should run next.
 * <p>
 * The scheduler is deliberately simplified. It only operates on an
 * abstract queue of {@link OSProcess} objects, and does not manage
 * real threads or CPU cores.
 */
public class Scheduler {

    private final Queue<OSProcess> readyQueue = new ArrayDeque<>();
    private SchedulingAlgorithm algorithm;
    private int timeQuantum;

    // Internal round-robin bookkeeping.
    private OSProcess currentProcess;
    private int remainingQuantum;

    public Scheduler(SchedulingAlgorithm algorithm) {
        this(algorithm, 3);
    }

    public Scheduler(SchedulingAlgorithm algorithm, int timeQuantum) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.timeQuantum = Math.max(1, timeQuantum);
    }

    public SchedulingAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SchedulingAlgorithm algorithm) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        // Reset round-robin state when the algorithm changes.
        currentProcess = null;
        remainingQuantum = 0;
    }

    public int getTimeQuantum() {
        return timeQuantum;
    }

    public void setTimeQuantum(int timeQuantum) {
        this.timeQuantum = Math.max(1, timeQuantum);
    }

    /**
     * Adds a process to the READY queue.
     */
    public void addProcess(OSProcess process) {
        if (process == null) {
            return;
        }
        if (!readyQueue.contains(process)) {
            readyQueue.offer(process);
        }
    }

    /**
     * Removes a process from the READY queue (e.g. when it terminates).
     */
    public void removeProcess(OSProcess process) {
        if (process == null) {
            return;
        }
        readyQueue.remove(process);
        if (process == currentProcess) {
            currentProcess = null;
            remainingQuantum = 0;
        }
    }

    /**
     * Returns the next process that should run according to the chosen
     * scheduling algorithm. The caller (typically {@link OSKernel}) is
     * responsible for updating process state and CPU accounting.
     */
    public OSProcess getNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }

        return switch (algorithm) {
            case FCFS -> fcfsNext();
            case ROUND_ROBIN -> roundRobinNext();
            case PRIORITY -> priorityNext();
            case SJF -> sjfNext();
        };
    }

    private OSProcess fcfsNext() {
        // First-Come-First-Served (non-preemptive):
        // always run the head of the queue, without rotating it.
        return readyQueue.peek();
    }

    private OSProcess roundRobinNext() {
        if (currentProcess == null || remainingQuantum <= 0 || !readyQueue.contains(currentProcess)) {
            // Move to the next process in the queue.
            if (currentProcess != null && readyQueue.contains(currentProcess)) {
                // Rotate current to the tail.
                readyQueue.remove(currentProcess);
                readyQueue.offer(currentProcess);
            }
            currentProcess = readyQueue.peek();
            remainingQuantum = timeQuantum;
        }
        if (currentProcess == null) {
            return null;
        }
        remainingQuantum--;
        return currentProcess;
    }

    private OSProcess priorityNext() {
        // Highest priority value wins; FCFS within equal priority.
        Optional<OSProcess> best = readyQueue.stream()
                .max(Comparator.comparingInt(OSProcess::getPriority));
        return best.orElse(null);
    }

    private OSProcess sjfNext() {
        // Shortest Job First based on remaining estimated burst time.
        Optional<OSProcess> best = readyQueue.stream()
                .min(Comparator.comparingInt(p -> {
                    int remaining = p.getEstimatedBurstTime() - (int) p.getCpuTimeUsed();
                    return Math.max(remaining, 0);
                }));
        return best.orElse(null);
    }
}
