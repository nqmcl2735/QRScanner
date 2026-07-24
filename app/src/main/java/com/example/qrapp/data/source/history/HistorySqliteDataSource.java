package com.example.qrapp.data.source.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.QRContentType;
import com.example.qrapp.data.model.QRHistoryItem;
import java.util.ArrayList;
import java.util.List;

public class HistorySqliteDataSource extends SQLiteOpenHelper implements IHistoryDataSource {
    private static final String DB_NAME = "qr_history.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "history";
    private static final String COL_ID = "id";
    private static final String COL_CONTENT = "content";
    private static final String COL_TYPE = "type";
    private static final String COL_SOURCE = "source";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_IMAGE_PATH = "image_path";

    public HistorySqliteDataSource(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONTENT + " TEXT NOT NULL, " +
                COL_TYPE + " TEXT NOT NULL, " +
                COL_SOURCE + " TEXT NOT NULL, " +
                COL_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_IMAGE_PATH + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    @Override
    public long insert(QRHistoryItem item) {
        ContentValues values = new ContentValues();
        values.put(COL_CONTENT, item.getContent());
        values.put(COL_TYPE, item.getType().name());
        values.put(COL_SOURCE, item.getSource().name());
        values.put(COL_TIMESTAMP, item.getTimestamp());
        values.put(COL_IMAGE_PATH, item.getImagePath());
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE, null, values);
        }
    }

    @Override
    public List<QRHistoryItem> getAll() {
        List<QRHistoryItem> items = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE, null, null, null, null, null, COL_TIMESTAMP + " DESC")) {
            while (cursor.moveToNext()) items.add(fromCursor(cursor));
        }
        return items;
    }

    @Override
    @Nullable
    public QRHistoryItem getById(long id) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) return fromCursor(cursor);
        }
        return null;
    }

    @Override
    public void delete(long id) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
        }
    }

    private QRHistoryItem fromCursor(Cursor cursor) {
        return new QRHistoryItem(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)),
                QRContentType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))),
                HistorySource.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE))),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)));
    }
}
