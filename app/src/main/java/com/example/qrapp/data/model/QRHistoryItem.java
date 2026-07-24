package com.example.qrapp.data.model;

public class QRHistoryItem {
    private long id;
    private final String content;
    private final QRContentType type;
    private final HistorySource source;
    private final long timestamp;
    private final String imagePath;

    public QRHistoryItem(long id, String content, QRContentType type, HistorySource source, long timestamp, String imagePath) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.source = source;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getContent() { return content; }
    public QRContentType getType() { return type; }
    public HistorySource getSource() { return source; }
    public long getTimestamp() { return timestamp; }
    public String getImagePath() { return imagePath; }
}
