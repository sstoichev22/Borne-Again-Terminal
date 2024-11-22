import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;

public class TerminalApp {
    private Path currentDirectory = Paths.get(System.getProperty("user.dir"));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TerminalApp::new);
    }

    public TerminalApp() {
        JFrame frame = new JFrame("Custom Terminal");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JTextArea terminal = new JTextArea();
        terminal.setBackground(Color.black);
        terminal.setForeground(Color.green);
        terminal.setEditable(false);
        terminal.setFont(new Font("Monospaced", Font.PLAIN, 16));
        terminal.setLineWrap(true);
        terminal.setWrapStyleWord(true);
        terminal.setMargin(new Insets(0, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(terminal);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);

        appendOutput(terminal, "Welcome to the Custom Terminal!\n");
        appendOutput(terminal, "Type 'help' for a list of commands.\n> ");

        terminal.setEditable(true);
        terminal.addKeyListener(new KeyAdapter() {
            private String currentCommand = "";

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    currentCommand = terminal.getText()
                            .substring(terminal.getText().lastIndexOf("\n") + 3)
                            .trim();
                    processCommand(terminal, currentCommand);
                }
            }
        });
    }

    private void appendOutput(JTextArea terminal, String message) {
        terminal.append(message);
        terminal.setCaretPosition(terminal.getDocument().getLength());
    }

    private void processCommand(JTextArea terminal, String command) {
        if (command.isEmpty()) return;

        appendOutput(terminal, "\n");
        String[] args = command.split(" ", 2);
        String cmd = args[0];
        String argument = args.length > 1 ? args[1] : "";

        try {
            switch (cmd) {
                case "help":
                    appendOutput(terminal, "Available commands:\n");
                    appendOutput(terminal, " - help: Show this help message\n");
                    appendOutput(terminal, " - clear: Clear the terminal\n");
                    appendOutput(terminal, " - exit: Close the terminal\n");
                    appendOutput(terminal, " - touch <filename>: Create a new file\n");
                    appendOutput(terminal, " - cat <filename>: Show the contents of a file\n");
                    appendOutput(terminal, " - nano <filename>: Open a new terminal to edit a file\n");
                    appendOutput(terminal, " - cd <directory>: Change directory\n");
                    appendOutput(terminal, " - ls: List contents of the current directory\n");
                    appendOutput(terminal, " - mkdir <directory>: Create a new directory\n");
                    appendOutput(terminal, " - rm <filename>: Remove a file\n");
                    appendOutput(terminal, " - rm -r <directory>: Remove a directory and its contents without confirmation\n");
                    break;

                case "clear":
                    terminal.setText("");
                    appendOutput(terminal, "Welcome to the Custom Terminal!\n");
                    appendOutput(terminal, "Type 'help' for a list of commands.\n");
                    break;

                case "exit":
                    appendOutput(terminal, "Goodbye!\n");
                    System.exit(0);
                    break;

                case "touch":
                    if (argument.isEmpty()) {
                        appendOutput(terminal, "Error: Missing filename.\n");
                    } else {
                        Files.createFile(currentDirectory.resolve(argument));
                        appendOutput(terminal, "File created: " + argument + "\n");
                    }
                    break;

                case "mkdir":
                    if (argument.isEmpty()) {
                        appendOutput(terminal, "Error: Missing directory name.\n");
                    } else {
                        Files.createDirectory(currentDirectory.resolve(argument));
                        appendOutput(terminal, "Directory created: " + argument + "\n");
                    }
                    break;

                case "rm":
                    if (argument.isEmpty()) {
                        appendOutput(terminal, "Error: Missing file or directory name.\n");
                    } else if (argument.startsWith("-r ")) {
                        String dirName = argument.substring(3).trim();
                        Path dirPath = currentDirectory.resolve(dirName);
                        if (Files.isDirectory(dirPath)) {
                            deleteDirectory(dirPath);
                            appendOutput(terminal, "Directory and contents deleted: " + dirName + "\n");
                        } else {
                            appendOutput(terminal, "Error: " + dirName + " is not a directory.\n");
                        }
                    } else {
                        Path targetPath = currentDirectory.resolve(argument);
                        if (Files.isDirectory(targetPath)) {
                            if (Files.list(targetPath).findAny().isPresent()) {
                                int response = JOptionPane.showConfirmDialog(null,
                                        "The directory " + argument + " is not empty. Delete contents?",
                                        "Confirm Delete",
                                        JOptionPane.YES_NO_OPTION);
                                if (response == JOptionPane.YES_OPTION) {
                                    deleteDirectory(targetPath);
                                    appendOutput(terminal, "Directory deleted: " + argument + "\n");
                                } else {
                                    appendOutput(terminal, "Deletion cancelled.\n");
                                }
                            } else {
                                Files.delete(targetPath);
                                appendOutput(terminal, "Directory deleted: " + argument + "\n");
                            }
                        } else if (Files.exists(targetPath)) {
                            Files.delete(targetPath);
                            appendOutput(terminal, "File deleted: " + argument + "\n");
                        } else {
                            appendOutput(terminal, "Error: File or directory not found.\n");
                        }
                    }
                    break;

                case "cat":
                    if (argument.isEmpty()) {
                        appendOutput(terminal, "Error: Missing filename.\n");
                    } else {
                        Path filePath = currentDirectory.resolve(argument);
                        if (Files.exists(filePath)) {
                            String content = Files.readString(filePath);
                            appendOutput(terminal, content + "\n");
                        } else {
                            appendOutput(terminal, "Error: File not found.\n");
                        }
                    }
                    break;

                case "nano":
                    if (argument.isEmpty()) {
                        appendOutput(terminal, "Error: Missing filename.\n");
                    } else {
                        openNanoWindow(argument);
                    }
                    break;

                case "cd":
                    if (argument.isEmpty()) {
                        appendOutput(terminal, "Error: Missing directory.\n");
                    } else {
                        Path newDir = currentDirectory.resolve(argument).normalize();
                        if (Files.isDirectory(newDir)) {
                            currentDirectory = newDir;
                            appendOutput(terminal, "Current directory: " + currentDirectory + "\n");
                        } else {
                            appendOutput(terminal, "Error: Directory not found.\n");
                        }
                    }
                    break;

                case "ls":
                    String contents = Files.list(currentDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .collect(Collectors.joining("\n"));
                    appendOutput(terminal, contents + "\n");
                    break;
                case "skibidi":
                    try {
                        ImageIcon originalIcon = new ImageIcon(getClass().getResource("skibidi.png"));
                        Image originalImage = originalIcon.getImage();
                        int maxWidth = 1920;
                        int maxHeight = 1080;
                        int originalWidth = originalIcon.getIconWidth();
                        int originalHeight = originalIcon.getIconHeight();
                        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
                        int scaledWidth = (int) (originalWidth * scale);
                        int scaledHeight = (int) (originalHeight * scale);
                        Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                        ImageIcon scaledIcon = new ImageIcon(scaledImage);

                        JFrame skibidiFrame = new JFrame("Skibidi");
                        skibidiFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        skibidiFrame.setSize(scaledWidth, scaledHeight);
                        skibidiFrame.setLocationRelativeTo(null);

                        JLabel imageLabel = new JLabel(scaledIcon);
                        skibidiFrame.add(imageLabel);
                        skibidiFrame.setVisible(true);
                    } catch (Exception e) {
                        appendOutput(terminal, "Error: Unable to load image. " + e.getMessage() + "\n");
                    }
                    break;


                default:
                    appendOutput(terminal, "Unknown command: " + command + "\n");
                    break;
            }
        } catch (IOException e) {
            appendOutput(terminal, "Error: " + e.getMessage() + "\n");
        }
        appendOutput(terminal, "> ");
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void openNanoWindow(String filename) {
        JFrame nanoFrame = new JFrame("nano - " + filename);
        nanoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        nanoFrame.setSize(600, 400);
        nanoFrame.setLocationRelativeTo(null);

        JTextArea nanoEditor = new JTextArea();
        nanoEditor.setBackground(Color.black);
        nanoEditor.setForeground(Color.green);
        nanoEditor.setFont(new Font("Monospaced", Font.PLAIN, 16));
        nanoEditor.setLineWrap(true);
        nanoEditor.setWrapStyleWord(true);

        Path filePath = currentDirectory.resolve(filename);
        if (Files.exists(filePath)) {
            try {
                String content = Files.readString(filePath);
                nanoEditor.setText(content);
            } catch (IOException e) {
                nanoEditor.setText("Error loading file: " + e.getMessage());
            }
        }

        JScrollPane scrollPane = new JScrollPane(nanoEditor);
        nanoFrame.add(scrollPane);

        JButton saveButton = new JButton("Save and Exit");
        saveButton.addActionListener(e -> {
            try {
                Files.writeString(filePath, nanoEditor.getText());
                nanoFrame.dispose();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        nanoFrame.add(saveButton, BorderLayout.SOUTH);
        nanoFrame.setVisible(true);
    }
}
