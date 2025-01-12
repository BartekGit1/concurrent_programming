package p;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Main {
    public static void main(String[] args) {

        JFrame frame = new JFrame("File Upload Simulation");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Client ID", "File Sizes", "Enqueue Time"}, 0);
        JTable clientTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(clientTable);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new GridLayout(1, 5));

        JButton addButton = new JButton("Add User");
        JButton startButton = new JButton("Start Simulation");

        FileUploadSystem system = new FileUploadSystem(logArea, tableModel, progressPanel);

        for (int i = 0; i < 10; i++) {
            system.addClient();
        }

        addButton.addActionListener(e -> system.addClient());
        startButton.addActionListener(e -> {
            SwingUtilities.invokeLater(system::updateTable);
            system.processUploads();
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(addButton);
        controlPanel.add(startButton);

        frame.setLayout(new BorderLayout());
        frame.add(logScrollPane, BorderLayout.CENTER);
        frame.add(tableScrollPane, BorderLayout.EAST);
        frame.add(progressPanel, BorderLayout.NORTH);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
