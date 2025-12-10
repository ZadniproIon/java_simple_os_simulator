package os.apps;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import os.memory.MemoryManager;
import os.memory.MemoryPage;
import os.process.OSKernel;
import os.process.OSProcess;

import java.util.HashMap;
import java.util.Map;

/**
 * Shows a live view of the simulated memory subsystem: page frames, usage,
 * and synthetic page fault / TLB statistics.
 */
public class SystemMonitorApp implements OSApplication {

    private final OSKernel kernel;
    private final MemoryManager memoryManager;

    private BorderPane root;
    private Label totalLabel;
    private Label usedLabel;
    private Label freeLabel;
    private Label cpuLabel;
    private FlowPane pageGrid;
    private Timeline refreshTimeline;

    private final Map<Integer, Color> pidColors = new HashMap<>();

    public SystemMonitorApp(OSKernel kernel) {
        this.kernel = kernel;
        this.memoryManager = kernel.getMemoryManager();
    }

    @Override
    public String getName() {
        return "System Monitor";
    }

    @Override
    public Parent createContent() {
        if (root != null) {
            return root;
        }

        totalLabel = new Label();
        usedLabel = new Label();
        freeLabel = new Label();

        HBox memorySummary = new HBox(15, totalLabel, usedLabel, freeLabel);
        memorySummary.setAlignment(Pos.CENTER_LEFT);

        cpuLabel = new Label();

        VBox statsBox = new VBox(5, cpuLabel);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        VBox topBox = new VBox(8, new Label("System statistics"), memorySummary, statsBox);
        topBox.setPadding(new Insets(10));

        pageGrid = new FlowPane();
        pageGrid.setHgap(4);
        pageGrid.setVgap(4);
        pageGrid.setPadding(new Insets(10));

        root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(pageGrid);

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshView()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        refreshView();
        return root;
    }

    private void refreshView() {
        int total = memoryManager.getTotalMemory();
        int used = memoryManager.getUsedMemory();
        int free = memoryManager.getFreeMemory();

        totalLabel.setText("Total RAM: " + total + " MB");
        usedLabel.setText("Used: " + used + " MB");
        freeLabel.setText("Free: " + free + " MB");

        double cpuUsage = kernel.getCpuUsagePercentage(120);
        cpuLabel.setText(String.format("CPU activity: %.0f%%", cpuUsage));

        pageGrid.getChildren().clear();
        for (MemoryPage page : memoryManager.getPages()) {
            Rectangle rect = new Rectangle(22, 22);
            OSProcess owner = page.getOwner();
            if (owner == null) {
                rect.setFill(Color.web("#2d2d2d"));
            } else {
                rect.setFill(colorForPid(owner.getPid()));
            }
            rect.setStroke(Color.BLACK);

            pageGrid.getChildren().add(rect);
        }
    }

    private Color colorForPid(int pid) {
        return pidColors.computeIfAbsent(pid, p -> {
            int base = Math.abs(p * 37);
            double r = ((base >> 0) & 0xFF) / 255.0;
            double g = ((base >> 8) & 0xFF) / 255.0;
            double b = ((base >> 16) & 0xFF) / 255.0;
            return Color.color(0.3 + 0.7 * r, 0.3 + 0.7 * g, 0.3 + 0.7 * b);
        });
    }

    @Override
    public void onStop() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}
