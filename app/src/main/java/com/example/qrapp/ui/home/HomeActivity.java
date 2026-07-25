package com.example.qrapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.qrapp.R;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.ParsedQRContent;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.databinding.ActivityHomeBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.detail.QRDetailActivity;
import com.example.qrapp.ui.generator.QRGeneratorActivity;
import com.example.qrapp.ui.history.HistoryActivity;
import com.example.qrapp.ui.scanner.QRScannerActivity;
import com.example.qrapp.util.QRContentParser;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends BaseActivity {
    private ActivityHomeBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<ScanOptions> cameraScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) return;
                String scannedText = result.getContents();
                saveAndOpenDetail(scannedText);
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.heroHeader);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        insetsController.setAppearanceLightStatusBars(false);
        binding.cardGenerate.setOnClickListener(view -> startActivity(new Intent(this, QRGeneratorActivity.class)));
        binding.cardCameraScan.setOnClickListener(view -> launchCameraScanner());
        binding.cardScan.setOnClickListener(view -> startActivity(new Intent(this, QRScannerActivity.class)));
        binding.cardHistory.setOnClickListener(view -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private void launchCameraScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.camera_scan_description));
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);
        cameraScanLauncher.launch(options);
    }

    private void saveAndOpenDetail(String scannedText) {
        HistoryRepository historyRepository = new HistoryRepository(this, new HistorySqliteDataSource(this));
        executor.execute(() -> {
            try {
                ParsedQRContent parsed = QRContentParser.parse(scannedText);
                long id = historyRepository.saveEntry(scannedText, parsed.getType(), HistorySource.SCANNED, null).getId();
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, QRDetailActivity.class);
                    intent.putExtra(QRDetailActivity.EXTRA_HISTORY_ID, id);
                    startActivity(intent);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                        R.string.camera_scan_error, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
