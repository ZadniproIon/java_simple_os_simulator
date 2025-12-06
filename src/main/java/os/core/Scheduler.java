package os.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Cooperative scheduler that can simulate FCFS or Round Robin.
 */
public class Scheduler {
    private final Deque<OSProcess> readyQueue = new ArrayDeque<>();
    private SchedulingAlgorithm algorithm = SchedulingAlgorithm.ROUND_ROBIN;

    public synchronized void addProcess(OSProcess process) {
        if (process == null || readyQueue.contains(process)) {
            return;
        }
        readyQueue.offer(process);
    }

    public synchronized void removeProcess(OSProcess process) {
        readyQueue.remove(process);
    }

    public synchronized OSProcess scheduleNext() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        if (algorithm == SchedulingAlgorithm.FCFS) {
            return readyQueue.peek();
        }
        return readyQueue.poll();
    }

    public synchronized void requeue(OSProcess process) {
        if (process == null) {
            return;
        }
        if (algorithm == SchedulingAlgorithm.ROUND_ROBIN && !readyQueue.contains(process)) {
            readyQueue.offer(process);
        }
    }

    public synchronized SchedulingAlgorithm getAlgorithm() {
        return algorithm;
    }

    public synchronized void setAlgorithm(SchedulingAlgorithm algorithm) {
        this.algorithm = Objects.requireNonNull(algorithm);
    }
}
