package com.example.qrapp.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.QRContentType;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.source.history.IHistoryDataSource;
import com.example.qrapp.util.ImageFileStore;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class HistoryRepository {
    private final Context appContext;
    private final IHistoryDataSource dataSource;

    public HistoryRepository(Context context, IHistoryDataSource dataSource) {
        this.appContext = context.getApplicationContext();
        this.dataSource = dataSource;
    }

    /** Lưu ảnh QR + ghi 1 dòng lịch sử. Phải gọi trên background thread. */
    public QRHistoryItem saveEntry(String content, QRContentType type, HistorySource source, Bitmap bitmap) throws IOException {
        String imagePath = null;
        if (bitmap != null) {
            File file = ImageFileStore.saveSnapshot(appContext, bitmap);
            imagePath = file.getAbsolutePath();
        }
        QRHistoryItem item = new QRHistoryItem(0, content, type, source, System.currentTimeMillis(), imagePath);
        long id = dataSource.insert(item);
        item.setId(id);
        return item;
    }

    public List<QRHistoryItem> getAll() {
        return dataSource.getAll();
    }

    public QRHistoryItem getById(long id) {
        return dataSource.getById(id);
    }

    public void delete(QRHistoryItem item) {
        dataSource.delete(item.getId());
        ImageFileStore.delete(item.getImagePath());
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Export all history to CSV format. Returns the CSV string. */
    public String exportToCsv() {
        List<QRHistoryItem> items = dataSource.getAll();
        StringBuilder sb = new StringBuilder();
        sb.append("id,content,type,source,timestamp\n"); // header
        for (QRHistoryItem item : items) {
            sb.append(escapeCsv(item.getId()))
              .append(",")
              .append(escapeCsv(item.getContent()))
              .append(",")
              .append(item.getType().name())
              .append(",")
              .append(item.getSource().name())
              .append(",")
              .append(item.getTimestamp())
              .append("\n");
        }
        return sb.toString();
    }

    /** Import history from CSV content. Returns the number of imported entries. */
    public int importFromCsv(String csvContent) {
        if (csvContent == null || csvContent.isEmpty()) return 0;
        String[] lines = csvContent.split("\n");
        if (lines.length <= 1) return 0; // only header or empty

        int count = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Simple CSV parsing assuming no complex quotes for enum/numbers
            // For content, it might have commas and quotes if escaped
            // Basic regex or simple split won't perfectly handle complex CSV, 
            // but let's implement a decent simple parser.
            List<String> cols = parseCsvLine(line);
            if (cols.size() < 5) continue;
            
            try {
                // id,content,type,source,timestamp
                String content = cols.get(1);
                QRContentType type = QRContentType.valueOf(cols.get(2));
                HistorySource source = HistorySource.valueOf(cols.get(3));
                long timestamp = Long.parseLong(cols.get(4));

                QRHistoryItem item = new QRHistoryItem(0, content, type, source, timestamp, null);
                dataSource.insert(item);
                count++;
            } catch (Exception e) {
                // ignore parsing errors for a row
                e.printStackTrace();
            }
        }
        return count;
    }

    private List<String> parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    current.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
