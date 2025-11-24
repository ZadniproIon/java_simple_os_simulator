package os.apps;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import os.core.OSKernel;
import os.core.OSProcess;

/**
 * Displays the simulated process table and allows terminating processes.
 */
public class TaskManagerApp implements OSApplication {
    private final OSKernel kernel;
    private BorderPane root;
    private TableView<OSProcess> tableView;
    private Timeline refreshTimeline;

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
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setItems(kernel.getProcesses());

        TableColumn<OSProcess, Number> pidColumn = new TableColumn<>("PID");
        pidColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPid()));

        TableColumn<OSProcess, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<OSProcess, String> stateColumn = new TableColumn<>("State");
        stateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getState().name()));

        TableColumn<OSProcess, Number> memoryColumn = new TableColumn<>("Memory (MB)");
        memoryColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAllocatedMemory()));

        TableColumn<OSProcess, Number> cpuColumn = new TableColumn<>("CPU Ticks");
        cpuColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCpuTimeUsed()));

        tableView.getColumns().addAll(pidColumn, nameColumn, stateColumn, memoryColumn, cpuColumn);

        Button killButton = new Button("Kill Process");
        killButton.setOnAction(e -> killSelectedProcess());

        HBox controls = new HBox(10, killButton);
        controls.setPadding(new Insets(8));

        root = new BorderPane(tableView);
        root.setBottom(controls);
        root.setTop(new Label("Running processes"));

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> tableView.refresh()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        return root;
    }

    private void killSelectedProcess() {
        OSProcess selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            kernel.killProcess(selected.getPid());
        }
    }

    @Override
    public void onStop() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}
