package com.example.qrapp.ui.generator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.qrapp.R;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.repository.QRGeneratorRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.data.source.qrlib.ZXingQRCodeProvider;
import com.example.qrapp.data.source.storage.MediaStoreDataSource;
import com.example.qrapp.databinding.ActivityQrGeneratorBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.viewmodel.ViewModelFactory;
import com.example.qrapp.util.QRActionBinder;
import com.example.qrapp.util.ShareUtil;
import com.google.android.material.snackbar.Snackbar;

public class QRGeneratorActivity extends BaseActivity {
    private ActivityQrGeneratorBinding binding;
    private QRGeneratorViewModel viewModel;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrGeneratorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.toolbar);

        QRGeneratorRepository repository = new QRGeneratorRepository(
                new ZXingQRCodeProvider(), new MediaStoreDataSource(this));
        HistoryRepository historyRepository = new HistoryRepository(this, new HistorySqliteDataSource(this));
        viewModel = new ViewModelProvider(this, ViewModelFactory.forGenerator(repository, historyRepository)).get(QRGeneratorViewModel.class);
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) viewModel.saveQRCodeToStorage();
            else showMessage(getString(R.string.permission_denied));
        });

        binding.toolbar.setNavigationContentDescription(R.string.navigate_up);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        
        binding.colorForeground.setOnClickListener(v -> showColorPicker(true));
        binding.colorBackground.setOnClickListener(v -> showColorPicker(false));
        
        binding.btnGenerate.setOnClickListener(view -> {
            binding.inputLayout.setError(null);
            viewModel.generateQRCode(String.valueOf(binding.editContent.getText()));
        });
        binding.btnSave.setOnClickListener(view -> saveWithPermission());
        binding.btnShare.setOnClickListener(view -> ShareUtil.showShareChooser(this,
                String.valueOf(binding.editContent.getText()), viewModel.getCurrentBitmap()));
        observeState();
    }

    private final int[] PRESET_COLORS = {
        0xFF000000, 0xFF17211B, 0xFF1A237E, 0xFF0D47A1,
        0xFF006064, 0xFF1B5E20, 0xFF33691E, 0xFFE65100,
        0xFFBF360C, 0xFF880E4F, 0xFF4A148C, 0xFF311B92,
        0xFFFFFFFF, 0xFFF5F5F5, 0xFFFFF8E1, 0xFFE8F5E9,
        0xFFE3F2FD, 0xFFFCE4EC, 0xFFEDE7F6, 0xFFE0F2F1
    };

    private void showColorPicker(boolean isForeground) {
        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(32, 32, 32, 32);
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.color_picker_title)
                .setView(grid)
                .create();
                
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        
        for (int color : PRESET_COLORS) {
            View colorView = new View(this);
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);
            
            colorView.setBackgroundResource(R.drawable.bg_soft_circle);
            colorView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            if (color == 0xFFFFFFFF || color == 0xFFF5F5F5) {
                // Add a border if it's very light (optional, but since we don't have a border drawable easily, just let it be)
            }
            
            colorView.setOnClickListener(v -> {
                if (isForeground) {
                    viewModel.setForegroundColor(color);
                    binding.colorForeground.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                } else {
                    viewModel.setBackgroundColor(color);
                    binding.colorBackground.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                }
                dialog.dismiss();
            });
            grid.addView(colorView);
        }
        dialog.show();
    }

    private void observeState() {
        viewModel.getQrBitmap().observe(this, bitmap -> {
            binding.imageQr.setImageBitmap(bitmap);
            binding.imageQr.setVisibility(View.VISIBLE);
            binding.emptyPreview.setVisibility(View.GONE);
            binding.btnSave.setEnabled(true);
            binding.btnShare.setEnabled(true);
        });
        viewModel.getParsedContent().observe(this, parsed -> QRActionBinder.bind(this, binding.layoutQrActions, parsed));
        viewModel.getSavedUri().observe(this, uri -> showMessage(getString(R.string.saved_success)));
        viewModel.getError().observe(this, message -> {
            if (message.startsWith("Vui lòng nhập")) binding.inputLayout.setError(message);
            else showMessage(message);
        });
        viewModel.getLoading().observe(this, loading -> {
            boolean active = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(active ? View.VISIBLE : View.GONE);
            binding.btnGenerate.setEnabled(!active);
            binding.btnSave.setEnabled(!active && viewModel.getQrBitmap().getValue() != null);
        });
    }

    private void saveWithPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            viewModel.saveQRCodeToStorage();
        }
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).setAnchorView(binding.btnSave).show();
    }
}
