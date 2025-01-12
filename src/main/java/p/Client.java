package p;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Client implements Comparable<Client> {
    int id;
    Queue<File> files;
    long enqueueTime;

    public Client(int id, List<File> files) {
        this.id = id;
        files.sort(Comparator.comparingInt(f -> f.size));
        this.files = new LinkedList<>(files);
        this.enqueueTime = System.currentTimeMillis();
    }

    public File getNextFile() {
        return files.poll();
    }

    public boolean hasFiles() {
        return !files.isEmpty();
    }

    public String getFileSizes() {
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.append(file.size).append(", ");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "";
    }

    @Override
    public int compareTo(Client other) {
        long waitingTimeThis = (System.currentTimeMillis() - this.enqueueTime) / 1000;
        long waitingTimeOther = (System.currentTimeMillis() - other.enqueueTime) / 1000;

        File thisFile = this.files.peek();
        File otherFile = other.files.peek();

        if (thisFile == null || otherFile == null) {
            return 0;
        }

        double priorityThis = Math.log(waitingTimeThis) / Math.sqrt(FileUploadSystem.activeClients.get()) + Math.sqrt(FileUploadSystem.activeClients.get()) / (double) thisFile.size;
        double priorityOther = Math.log(waitingTimeOther) / Math.sqrt(FileUploadSystem.activeClients.get()) + Math.sqrt(FileUploadSystem.activeClients.get()) / (double) otherFile.size;

        return Double.compare(priorityOther, priorityThis);
    }
}
