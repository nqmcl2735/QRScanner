package com.example.qrapp.ui.scanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import com.example.qrapp.R;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.repository.QRScannerRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.data.source.qrlib.ZXingQRCodeProvider;
import com.example.qrapp.data.source.storage.MediaStoreDataSource;
import com.example.qrapp.databinding.ActivityQrScannerBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.viewmodel.ViewModelFactory;
import com.example.qrapp.util.QRActionBinder;
import com.example.qrapp.util.ShareUtil;
import com.google.android.material.snackbar.Snackbar;

public class QRScannerActivity extends BaseActivity {
    private ActivityQrScannerBinding binding;
    private QRScannerViewModel viewModel;
    private ActivityResultLauncher<String> imagePicker;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.toolbar);

        QRScannerRepository repository = new QRScannerRepository(
                new ZXingQRCodeProvider(), new MediaStoreDataSource(this));
        HistoryRepository historyRepository = new HistoryRepository(this, new HistorySqliteDataSource(this));
        viewModel = new ViewModelProvider(this, ViewModelFactory.forScanner(repository, historyRepository)).get(QRScannerViewModel.class);
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImageSelected);

        binding.toolbar.setNavigationContentDescription(R.string.navigate_up);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.btnChooseImage.setOnClickListener(view -> imagePicker.launch("image/*"));
        binding.btnScan.setOnClickListener(view -> viewModel.scanSelectedImage());
        binding.btnCopy.setOnClickListener(view -> copyResult());
        binding.btnShare.setOnClickListener(view -> ShareUtil.showShareChooser(this,
                viewModel.getDecodedText().getValue(), viewModel.getCurrentBitmap()));
        observeState();
    }

    private void onImageSelected(Uri uri) {
        if (uri != null) viewModel.setSelectedImage(uri);
    }

    private void observeState() {
        viewModel.getSelectedImageUri().observe(this, uri -> {
            binding.imageSelected.setImageURI(uri);
            binding.imageSelected.setVisibility(View.VISIBLE);
            binding.emptyImage.setVisibility(View.GONE);
            binding.btnScan.setEnabled(true);
        });
        viewModel.getDecodedText().observe(this, text -> {
            boolean hasResult = text != null && !text.isEmpty();
            binding.textResult.setText(hasResult ? text : getString(R.string.result_empty));
            binding.textResult.setTextColor(ContextCompatHelper.color(this, hasResult ? R.color.text_primary : R.color.text_secondary));
            binding.btnCopy.setEnabled(hasResult);
            binding.btnShare.setEnabled(hasResult);
        });
        viewModel.getParsedContent().observe(this, parsed -> QRActionBinder.bind(this, binding.layoutQrActions, parsed));
        viewModel.getError().observe(this, this::showMessage);
        viewModel.getLoading().observe(this, loading -> {
            boolean active = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(active ? View.VISIBLE : View.GONE);
            binding.btnChooseImage.setEnabled(!active);
            binding.btnScan.setEnabled(!active && viewModel.getSelectedImageUri().getValue() != null);
        });
    }

    private void copyResult() {
        String text = viewModel.getDecodedText().getValue();
        if (text == null || text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.result_title), text));
        showMessage(getString(R.string.copied));
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).setAnchorView(binding.btnScan).show();
    }

    private static final class ContextCompatHelper {
        static int color(Context context, int colorRes) {
            return androidx.core.content.ContextCompat.getColor(context, colorRes);
        }
    }
}
