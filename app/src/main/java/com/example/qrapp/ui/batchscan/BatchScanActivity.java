package com.example.qrapp.ui.batchscan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.qrapp.R;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.detail.QRDetailActivity;
import com.example.qrapp.util.QRContentParser;
import com.google.android.material.appbar.MaterialToolbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchScanActivity extends BaseActivity {

    private TextView textCounter;
    private RecyclerView recyclerResults;
    private Button btnStart, btnDone;
    private BatchScanAdapter adapter;
    private int scanCount = 0;
    private HistoryRepository historyRepository;
    private ExecutorService executorService;
    private Handler mainHandler;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    handleScanResult(result.getContents());
                    // relaunch scanner
                    launchScanner();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_scan);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        applySystemBars(findViewById(android.R.id.content), toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        textCounter = findViewById(R.id.text_counter);
        recyclerResults = findViewById(R.id.recycler_results);
        btnStart = findViewById(R.id.btn_start);
        btnDone = findViewById(R.id.btn_done);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        historyRepository = new HistoryRepository(this, new HistorySqliteDataSource(this));

        adapter = new BatchScanAdapter(this::openDetail);
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerResults.setAdapter(adapter);

        btnStart.setOnClickListener(v -> launchScanner());
        btnDone.setOnClickListener(v -> finish());
        
        updateCounter();
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("H\u01b0\u1edbng camera v\u00e0o m\u00e3 QR");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void handleScanResult(String text) {
        ParsedQRContent parsed = QRContentParser.parse(text);
        
        executorService.execute(() -> {
            try {
                QRHistoryItem item = historyRepository.saveEntry(text, parsed.getType(), HistorySource.SCANNED, null);
                mainHandler.post(() -> {
                    scanCount++;
                    updateCounter();
                    adapter.addEntry(item);
                    recyclerResults.scrollToPosition(0);
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, R.string.batch_scan_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openDetail(QRHistoryItem entry) {
        Intent intent = new Intent(this, QRDetailActivity.class);
        intent.putExtra(QRDetailActivity.EXTRA_HISTORY_ID, entry.getId());
        startActivity(intent);
    }

    private void updateCounter() {
        if (scanCount == 0) {
            textCounter.setText(R.string.batch_scan_empty);
        } else {
            textCounter.setText(getString(R.string.batch_scan_counter, scanCount));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
