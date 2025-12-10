package os.apps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import os.memory.MemoryManager;
import os.process.OSKernel;
import os.process.OSProcess;
import os.users.AuthManager;
import os.users.UserAccount;
import os.vfs.VirtualDirectory;
import os.vfs.VirtualFile;
import os.vfs.VirtualFileSystem;
import os.vfs.VirtualNode;

/**
 * Simple shell-like terminal that executes a handful of commands against the
 * simulated OS kernel and virtual file system.
 */
public class TerminalApp implements OSApplication {

    private final OSKernel kernel;
    private final VirtualFileSystem fileSystem;
    private VirtualDirectory currentDirectory;

    private BorderPane root;
    private TextArea outputArea;
    private TextField inputField;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    public TerminalApp(OSKernel kernel) {
        this.kernel = kernel;
        this.fileSystem = kernel.getFileSystem();
        this.currentDirectory = kernel.getCurrentUserHomeDirectory();
    }

    @Override
    public String getName() {
        return "Terminal";
    }

    @Override
    public Parent createContent() {
        if (root != null) {
            return root;
        }
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.getStyleClass().add("terminal-output");

        inputField = new TextField();
        inputField.setPromptText("Type a command, e.g. help");
        inputField.getStyleClass().add("terminal-input");
        inputField.setOnAction(e -> executeInput());
        inputField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleHistoryNavigation);

        VBox bottom = new VBox(4, new Label("Terminal input"), inputField);
        bottom.setPadding(new Insets(6));

        root = new BorderPane(outputArea);
        root.setBottom(bottom);
        printBanner();
        return root;
    }

    private void printBanner() {
        appendLine("SimpleOS terminal. Type 'help' to list available commands.");
        printPrompt();
    }

    private void printPrompt() {
        append(String.format("\n%s$ ", currentDirectory.getPath()));
    }

    private void executeInput() {
        String line = inputField.getText();
        if (line == null) {
            return;
        }
        String trimmed = line.trim();
        append(line + "\n");
        inputField.clear();
        if (!trimmed.isEmpty()) {
            history.add(trimmed);
            historyIndex = history.size();
            handleCommand(trimmed);
        } else {
            printPrompt();
        }
    }

    private void handleHistoryNavigation(KeyEvent event) {
        if (history.isEmpty()) {
            return;
        }
        if (event.getCode() == KeyCode.UP) {
            historyIndex = Math.max(0, historyIndex - 1);
            inputField.setText(history.get(historyIndex));
            inputField.positionCaret(inputField.getText().length());
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            historyIndex = Math.min(history.size(), historyIndex + 1);
            if (historyIndex >= history.size()) {
                inputField.clear();
            } else {
                inputField.setText(history.get(historyIndex));
                inputField.positionCaret(inputField.getText().length());
            }
            event.consume();
        }
    }

    private void handleCommand(String commandLine) {
        String[] parts = commandLine.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String args = commandLine.length() > cmd.length()
                ? commandLine.substring(cmd.length()).trim()
                : "";

        switch (cmd) {
            case "help" -> showHelp();
            case "pwd" -> appendLine(currentDirectory.getPath());
            case "ls" -> listDirectory(args);
            case "cd" -> changeDirectory(args);
            case "cat" -> showFile(args);
            case "echo" -> appendLine(args);
            case "clear" -> {
                outputArea.clear();
            }
            case "whoami" -> showCurrentUser();
            case "ps" -> listProcesses();
            case "kill" -> killProcess(args);
            case "mem" -> showMemory();
            case "history" -> showHistory();
            default -> appendLine("Unknown command: " + cmd + ". Type 'help' for assistance.");
        }
        printPrompt();
    }

    private void showHelp() {
        appendLine("""
                Available commands:
                  help               - show this message
                  pwd                - print working directory
                  ls [dir]           - list files in current or provided directory
                  cd <dir>           - change directory (supports .. and /home/user style paths)
                  cat <file>         - display a text file
                  echo <text>        - print text back
                  ps                 - list running processes
                  kill <pid>         - terminate a process
                  mem                - show RAM usage
                  whoami             - display current user and role
                  history            - list previous commands
                  clear              - clear the terminal output""");
    }

    private void listDirectory(String path) {
        VirtualDirectory dir = path.isBlank() ? currentDirectory : resolveDirectory(path);
        if (dir == null) {
            appendLine("Directory not found: " + path);
            return;
        }
        List<String> entries = fileSystem.list(dir).stream()
                .map(node -> node instanceof VirtualDirectory ? node.getName() + "/" : node.getName())
                .collect(Collectors.toList());
        if (entries.isEmpty()) {
            appendLine("(empty)");
        } else {
            appendLine(String.join("  ", entries));
        }
    }

    private void changeDirectory(String path) {
        if (path.isBlank()) {
            currentDirectory = kernel.getCurrentUserHomeDirectory();
            appendLine("Changed to home directory.");
            return;
        }
        VirtualDirectory dir = resolveDirectory(path);
        if (dir == null || !fileSystem.exists(dir)) {
            appendLine("Directory not found: " + path);
            return;
        }
        currentDirectory = dir;
        appendLine("Directory changed to " + currentDirectory.getPath());
    }

    private void showFile(String path) {
        if (path.isBlank()) {
            appendLine("Usage: cat <file>");
            return;
        }
        VirtualFile file = resolveFile(path);
        if (file == null || !fileSystem.exists(file)) {
            appendLine("File not found: " + path);
            return;
        }
        try {
            appendLine(fileSystem.readFile(file));
        } catch (Exception e) {
            appendLine("Unable to read file: " + e.getMessage());
        }
    }

    private void showCurrentUser() {
        AuthManager auth = kernel.getAuthManager();
        UserAccount user = auth.getCurrentUser();
        if (user == null) {
            appendLine("Not logged in.");
        } else {
            appendLine(user.getUsername() + " (" + user.getRole() + ")");
        }
    }

    private void listProcesses() {
        appendLine("PID   STATE     MEM   NAME");
        for (OSProcess process : kernel.getProcesses()) {
            appendLine(String.format("%-5d %-9s %-5d %s",
                    process.getPid(),
                    process.getState(),
                    process.getSimulatedMemoryUsage(),
                    process.getName()));
        }
    }

    private void killProcess(String arg) {
        if (arg.isBlank()) {
            appendLine("Usage: kill <pid>");
            return;
        }
        try {
            int pid = Integer.parseInt(arg.trim());
            kernel.killProcess(pid);
            appendLine("Requested termination of PID " + pid);
        } catch (NumberFormatException ex) {
            appendLine("Invalid PID: " + arg);
        }
    }

    private void showMemory() {
        MemoryManager mm = kernel.getMemoryManager();
        appendLine(String.format("RAM: %d MB total, %d MB used, %d MB free",
                mm.getTotalMemory(), mm.getUsedMemory(), mm.getFreeMemory()));
    }

    private void showHistory() {
        if (history.isEmpty()) {
            appendLine("(no history)");
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            appendLine((i + 1) + ": " + history.get(i));
        }
    }

    private VirtualDirectory resolveDirectory(String rawPath) {
        String normalized = normalizePath(rawPath);
        try {
            return fileSystem.resolveDirectory(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private VirtualFile resolveFile(String rawPath) {
        String normalized = normalizePath(rawPath);
        try {
            return fileSystem.resolveFile(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return currentDirectory.getPath();
        }
        String base = rawPath.startsWith("/") ? rawPath : combine(currentDirectory.getPath(), rawPath);
        String[] tokens = base.split("/");
        Deque<String> stack = new ArrayDeque<>();
        for (String token : tokens) {
            if (token == null || token.isBlank() || ".".equals(token)) {
                continue;
            }
            if ("..".equals(token)) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
            } else {
                stack.add(token);
            }
        }
        if (stack.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", stack);
    }

    private String combine(String currentPath, String addition) {
        if ("/".equals(currentPath)) {
            return "/" + addition;
        }
        return currentPath + "/" + addition;
    }

    private void appendLine(String text) {
        append(text + "\n");
    }

    private void append(String text) {
        outputArea.appendText(text);
    }
}
