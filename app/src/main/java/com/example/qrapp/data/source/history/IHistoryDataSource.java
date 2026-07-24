package com.example.qrapp.data.source.history;

import com.example.qrapp.data.model.QRHistoryItem;
import java.util.List;

public interface IHistoryDataSource {
    long insert(QRHistoryItem item);
    List<QRHistoryItem> getAll();
    QRHistoryItem getById(long id);
    void delete(long id);
}
