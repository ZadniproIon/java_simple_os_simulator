package os.apps;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import os.process.OSKernel;
import os.process.OSProcess;
import os.process.Scheduler;
import os.process.SchedulingAlgorithm;

/**
 * Displays the simulated process table and allows terminating processes.
 * Also exposes scheduler controls and a small Gantt-style view of which
 * PID ran on each recent tick.
 */
public class TaskManagerApp implements OSApplication {
    private final OSKernel kernel;
    private BorderPane root;
    private TableView<OSProcess> tableView;
    private Timeline refreshTimeline;

    private ComboBox<SchedulingAlgorithm> algorithmBox;
    private Spinner<Integer> quantumSpinner;
    private Pane ganttPane;
    private Label ramSummaryLabel;
    private final Map<Integer, Color> pidColors = new HashMap<>();
    private static final int MAX_GANTT_TICKS = 20;

    public TaskManagerApp(OSKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public String getName() {
        return "Task Manager";
    }

    @Override
    public Parent createContent() {
        if (root != null) {
            return root;
        }
        tableView = new TableView<>();
        tableView.getStyleClass().add("data-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<OSProcess, Number> pidColumn = new TableColumn<>("PID");
        pidColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPid()));

        TableColumn<OSProcess, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<OSProcess, String> stateColumn = new TableColumn<>("State");
        stateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getState().name()));

        TableColumn<OSProcess, Number> memoryColumn = new TableColumn<>("Memory (MB)");
        memoryColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getSimulatedMemoryUsage()));

        TableColumn<OSProcess, String> uptimeColumn = new TableColumn<>("Uptime");
        uptimeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                formatDuration(Duration.ofMillis(System.currentTimeMillis() - data.getValue().getStartTimestamp()))));

        tableView.getColumns().addAll(pidColumn, nameColumn, stateColumn, memoryColumn, uptimeColumn);

        Button killButton = new Button("End task");
        killButton.getStyleClass().add("primary-button");
        killButton.setOnAction(e -> killSelectedProcess());

        algorithmBox = new ComboBox<>();
        algorithmBox.getItems().addAll(SchedulingAlgorithm.values());
        Scheduler scheduler = kernel.getScheduler();
        algorithmBox.getSelectionModel().select(scheduler.getAlgorithm());
        algorithmBox.valueProperty().addListener((obs, oldAlg, newAlg) -> {
            if (newAlg != null) {
                scheduler.setAlgorithm(newAlg);
            }
        });

        quantumSpinner = new Spinner<>();
        quantumSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, scheduler.getTimeQuantum()));
        quantumSpinner.valueProperty().addListener((obs, oldVal, newVal) -> scheduler.setTimeQuantum(newVal));

        HBox schedulerControls = new HBox(10,
                new Label("Algorithm:"), algorithmBox,
                new Label("Quantum:"), quantumSpinner,
                killButton);
        schedulerControls.setAlignment(Pos.CENTER_LEFT);
        schedulerControls.setPadding(new Insets(8));
        schedulerControls.getStyleClass().add("toolbar");

        ganttPane = new Pane();
        ganttPane.setPrefHeight(28);
        ganttPane.setMinHeight(28);
        ganttPane.setMaxHeight(28);
        ganttPane.setPadding(new Insets(4, 8, 8, 8));
        ganttPane.widthProperty().addListener((obs, oldVal, newVal) -> refreshGantt());

        ramSummaryLabel = new Label();
        ramSummaryLabel.getStyleClass().add("caption");

        VBox bottom = new VBox(4,
                new Label("Scheduler & history"),
                ramSummaryLabel,
                schedulerControls,
                ganttPane);

        Label heading = new Label("System processes");
        heading.getStyleClass().add("section-title");

        root = new BorderPane(tableView);
        root.setPadding(new Insets(12));
        root.setTop(heading);
        root.setBottom(bottom);

        refreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> refresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        refresh();
        return root;
    }

    private void refresh() {
        refreshData();
        refreshGantt();
    }

    private void refreshData() {
        tableView.getItems().setAll(kernel.getProcesses());
        int used = kernel.getMemoryManager().getUsedMemory();
        int total = kernel.getMemoryManager().getTotalMemory();
        int free = total - used;
        ramSummaryLabel.setText(String.format("RAM usage: %d / %d MB   (free %d MB)", used, total, free));
    }

    private void refreshGantt() {
        if (ganttPane == null) {
            return;
        }
        List<Integer> history = kernel.getRunHistory();
        int sliceLength = Math.min(MAX_GANTT_TICKS, history.size());
        int start = Math.max(0, history.size() - sliceLength);
        ganttPane.getChildren().clear();

        double available = Math.max(200, ganttPane.getWidth());
        if (available <= 200 && tableView != null) {
            available = Math.max(200, tableView.getWidth());
        }
        double spacing = 4;
        double cellWidth = Math.max(6, (available - spacing * (MAX_GANTT_TICKS - 1)) / MAX_GANTT_TICKS);

        int paddingSlots = MAX_GANTT_TICKS - sliceLength;
        double x = 0;
        for (int i = 0; i < MAX_GANTT_TICKS; i++) {
            Rectangle rect = new Rectangle(cellWidth, 20);
            rect.setManaged(false);
            int idx = i - paddingSlots;
            if (idx < 0) {
                rect.setFill(fadedColor(Color.web("#e0e0e0"), i));
            } else {
                int pid = history.get(start + idx);
                Color base = pid == 0 ? Color.web("#c7c7c7") : colorForPid(pid);
                rect.setFill(fadedColor(base, i));
            }
            rect.setLayoutX(x);
            rect.setLayoutY(4);
            ganttPane.getChildren().add(rect);
            x += cellWidth + spacing;
        }
    }

    private Color fadedColor(Color base, int positionIndex) {
        double ageFactor = (double) positionIndex / Math.max(1, MAX_GANTT_TICKS - 1);
        double blend = 0.15 + 0.55 * ageFactor;
        return base.interpolate(Color.WHITE, blend);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        long hours = absSeconds / 3600;
        long minutes = (absSeconds % 3600) / 60;
        long secs = absSeconds % 60;
        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, secs);
        }
        return String.format("%02dm %02ds", minutes, secs);
    }

    private Color colorForPid(int pid) {
        return pidColors.computeIfAbsent(pid, p -> {
            int base = Math.abs(p * 53);
            double r = ((base >> 0) & 0xFF) / 255.0;
            double g = ((base >> 8) & 0xFF) / 255.0;
            double b = ((base >> 16) & 0xFF) / 255.0;
            return Color.color(0.3 + 0.7 * r, 0.3 + 0.7 * g, 0.3 + 0.7 * b);
        });
    }

    private void killSelectedProcess() {
        OSProcess selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            kernel.killProcess(selected.getPid());
            refresh();
        }
    }

    @Override
    public void onStop() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}
