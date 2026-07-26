package com.example.qrapp.ui.batchscan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class BatchScanActivity extends BaseActivity {

    private DecoratedBarcodeView barcodeScanner;
    private TextView textCounter;
    private RecyclerView recyclerResults;
    private Button btnStartPause, btnDone;
    private BatchScanAdapter adapter;
    private int scanCount = 0;
    private HistoryRepository historyRepository;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isScanning = false;
    private long lastScanTime = 0;
    
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startScan();
                } else {
                    Toast.makeText(this, "Cần quyền máy ảnh để quét mã", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_scan);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        applySystemBars(findViewById(android.R.id.content), toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        barcodeScanner = findViewById(R.id.barcode_scanner);
        textCounter = findViewById(R.id.text_counter);
        recyclerResults = findViewById(R.id.recycler_results);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnDone = findViewById(R.id.btn_done);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        historyRepository = new HistoryRepository(this, new HistorySqliteDataSource(this));

        adapter = new BatchScanAdapter(this::openDetail);
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerResults.setAdapter(adapter);

        btnStartPause.setOnClickListener(v -> toggleScan());
        btnDone.setOnClickListener(v -> finish());
        
        setupScanner();
        updateCounter();
        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void setupScanner() {
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    long now = System.currentTimeMillis();
                    if (now - lastScanTime > 1500) { // Debounce scanning
                        lastScanTime = now;
                        handleScanResult(result.getText());
                    }
                }
            }
        });
    }

    private void toggleScan() {
        if (isScanning) {
            pauseScan();
        } else {
            startScan();
        }
    }

    private void startScan() {
        barcodeScanner.resume();
        isScanning = true;
        btnStartPause.setText("Tạm dừng");
    }

    private void pauseScan() {
        barcodeScanner.pause();
        isScanning = false;
        btnStartPause.setText(R.string.batch_scan_start);
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
    protected void onResume() {
        super.onResume();
        if (isScanning) {
            barcodeScanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
