package pl.lodz.p;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileUploadSystem {
    static AtomicInteger activeClients = new AtomicInteger(0);
    final PriorityBlockingQueue<Client> clientQueue = new PriorityBlockingQueue<>();
    List<Client> activeClientsList = new ArrayList<>();
    JTextArea logArea;
    DefaultTableModel tableModel;
    JPanel progressPanel;
    AtomicInteger clientIdCounter = new AtomicInteger(1);
    JLabel[] clientLabels;
    JProgressBar[] progressBars;
    JLabel[] fileLabels;

    public FileUploadSystem(JTextArea logArea, DefaultTableModel tableModel, JPanel progressPanel) {
        this.logArea = logArea;
        this.tableModel = tableModel;
        this.progressPanel = progressPanel;

        clientLabels = new JLabel[5];
        progressBars = new JProgressBar[5];
        fileLabels = new JLabel[5];

        for (int i = 0; i < 5; i++) {
            JPanel tile = new JPanel();
            tile.setLayout(new GridLayout(5, 1));
            clientLabels[i] = new JLabel("Idle");
            progressBars[i] = new JProgressBar();
            progressBars[i].setStringPainted(true);
            fileLabels[i] = new JLabel("No file");

            tile.add(clientLabels[i]);
            tile.add(progressBars[i]);
            tile.add(fileLabels[i]);

            progressPanel.add(tile);
        }
    }

    public void addClient() {
        int id = clientIdCounter.getAndIncrement();
        Random random = new Random();
        List<File> files = new ArrayList<>();
        int fileCount = random.nextInt(5) + 1;

        for (int i = 0; i < fileCount; i++) {
            files.add(new File(random.nextInt(100) + 1));
        }

        Client client = new Client(id, files);
        activeClients.incrementAndGet();
        clientQueue.add(client);
        updateTable();
    }

    public void processUploads() {
        for (int i = 0; i < 5; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                while (true) {
                    updateTable();
                    File file = null;
                    Client client;

                    synchronized (clientQueue) {
                        List<Client> clients = new ArrayList<>(clientQueue);
                        clients.sort(Client::compareTo);
                        clientQueue.clear();
                        clientQueue.addAll(clients);
                        client = clientQueue.poll();

                        if (client != null && client.hasFiles()) {
                            file = client.getNextFile();

                            if (client.hasFiles()) {
                                clientQueue.add(client);
                            } else {
                                activeClients.decrementAndGet();
                            }
                        }
                    }

                    if (file != null) {
                        String clientText = "Client " + client.id;
                        updateTile(threadIndex, clientText, "File size: " + file.size, 0);
                        uploadFile(file, threadIndex, client.id);
                    } else {
                        updateTile(threadIndex, "Idle", "No file", 0);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }).start();
        }
    }

    private void uploadFile(File file, int threadIndex, int clientId) {
        Random random = new Random();
        int uploadSpeed = (random.nextInt(500) + 300) * file.size;

        int steps = 100;
        int delayPerStep = uploadSpeed / steps;

        for (int i = 0; i <= steps; i++) {
            try {
                Thread.sleep(delayPerStep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int progress = i;
            SwingUtilities.invokeLater(() -> progressBars[threadIndex].setValue(progress));
        }

        log("Finished uploading file of size " + file.size + ", file owner id: " + clientId);
    }

    private void updateTile(int threadIndex, String clientText, String fileText, int progress) {
        SwingUtilities.invokeLater(() -> {
            clientLabels[threadIndex].setText(clientText);
            fileLabels[threadIndex].setText(fileText);
            progressBars[threadIndex].setValue(progress);
        });
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public void updateTable() {
        SwingUtilities.invokeLater(() -> {
            List<Client> allClients = new ArrayList<>(clientQueue);
            allClients.addAll(activeClientsList);

            allClients.removeIf(Objects::isNull);

            allClients.sort(Comparator.comparingInt(client -> client.id));

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int clientId = (int) tableModel.getValueAt(i, 0);
                Client client = getClientById(clientId, allClients);

                if (client == null || !client.hasFiles()) {
                    tableModel.removeRow(i);
                    i--;
                } else {
                    tableModel.setValueAt(client.getFileSizes(), i, 1);
                    tableModel.setValueAt(new Date(client.enqueueTime), i, 2);
                }
            }

            for (Client client : allClients) {
                if (client.hasFiles() && !clientInTable(client)) {
                    tableModel.addRow(new Object[]{
                            client.id,
                            client.getFileSizes(),
                            new Date(client.enqueueTime)
                    });
                }
            }
        });
    }

    private Client getClientById(int clientId, List<Client> allClients) {
        for (Client client : allClients) {
            if (client.id == clientId) {
                return client;
            }
        }
        return null;
    }

    private boolean clientInTable(Client client) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((int) tableModel.getValueAt(i, 0) == client.id) {
                return true;
            }
        }
        return false;
    }
}
