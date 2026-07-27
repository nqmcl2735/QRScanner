package com.example.qrapp.ui.generator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.example.qrapp.data.model.BarcodeType;
import com.google.android.material.chip.Chip;
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
        binding.btnGenerate.setOnClickListener(view -> {
            dismissContentInput();
            binding.inputLayout.setError(null);
            viewModel.generateQRCode(String.valueOf(binding.editContent.getText()));
        });
        binding.btnSave.setOnClickListener(view -> saveWithPermission());
        binding.btnShare.setOnClickListener(view -> ShareUtil.showShareChooser(this,
                String.valueOf(binding.editContent.getText()), viewModel.getCurrentBitmap()));
        
        setupBarcodeTypeChips();
        observeState();
    }

    private void setupBarcodeTypeChips() {
        for (BarcodeType type : BarcodeType.values()) {
            Chip chip = new Chip(this);
            chip.setText(type.getDisplayName());
            chip.setCheckable(true);
            if (type == BarcodeType.QR_CODE) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    viewModel.setBarcodeType(type);
                    binding.imageQr.setVisibility(View.GONE);
                    binding.emptyPreview.setVisibility(View.VISIBLE);
                    binding.btnSave.setEnabled(false);
                    binding.btnShare.setEnabled(false);
                    binding.layoutQrActions.setVisibility(View.GONE);
                }
            });
            binding.chipGroupBarcodeType.addView(chip);
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && binding != null
                && binding.editContent.hasFocus()) {
            Rect inputBounds = new Rect();
            binding.inputLayout.getGlobalVisibleRect(inputBounds);
            if (!inputBounds.contains((int) event.getRawX(), (int) event.getRawY())) {
                dismissContentInput();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void dismissContentInput() {
        binding.editContent.clearFocus();
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(binding.editContent.getWindowToken(), 0);
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
