package com.example.qrapp.ui.scanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.ViewModelProvider;
import com.example.qrapp.R;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.databinding.ActivityCameraScannerBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.detail.QRDetailActivity;
import com.example.qrapp.ui.viewmodel.ViewModelFactory;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class CameraScannerActivity extends BaseActivity {
    private ActivityCameraScannerBinding binding;
    private CameraScannerViewModel viewModel;

    private final ActivityResultLauncher<ScanOptions> cameraScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    finish();
                    return;
                }
                viewModel.saveScanResult(result.getContents(), result.getBarcodeImagePath());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationContentDescription(R.string.navigate_up);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        HistoryRepository historyRepository =
                new HistoryRepository(this, new HistorySqliteDataSource(this));
        viewModel = new ViewModelProvider(
                this, ViewModelFactory.forCameraScanner(historyRepository))
                .get(CameraScannerViewModel.class);
        observeState();

        if (savedInstanceState == null) {
            launchCameraScanner();
        }
    }

    private void launchCameraScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.camera_scan_description));
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);
        options.setCaptureActivity(CameraCaptureActivity.class);
        cameraScanLauncher.launch(options);
    }

    private void observeState() {
        viewModel.getLoading().observe(this, loading ->
                binding.progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.getSavedHistoryId().observe(this, historyId -> {
            if (historyId == null) return;
            viewModel.onNavigationHandled();
            Intent intent = new Intent(this, QRDetailActivity.class);
            intent.putExtra(QRDetailActivity.EXTRA_HISTORY_ID, historyId);
            startActivity(intent);
            finish();
        });
        viewModel.getSaveFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            viewModel.onErrorHandled();
            Toast.makeText(this, R.string.camera_scan_error, Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
