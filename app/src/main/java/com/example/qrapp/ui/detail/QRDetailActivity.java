package com.example.qrapp.ui.detail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.example.qrapp.R;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.databinding.ActivityQrDetailBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.viewmodel.ViewModelFactory;
import com.example.qrapp.util.QRActionBinder;
import com.example.qrapp.util.QRTypeStyle;
import com.example.qrapp.util.ShareUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class QRDetailActivity extends BaseActivity {
    public static final String EXTRA_HISTORY_ID = "extra_history_id";

    private ActivityQrDetailBinding binding;
    private QRDetailViewModel viewModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.toolbar);

        long historyId = getIntent().getLongExtra(EXTRA_HISTORY_ID, -1);
        HistoryRepository repository = new HistoryRepository(this, new HistorySqliteDataSource(this));
        viewModel = new ViewModelProvider(this, ViewModelFactory.forHistory(repository)).get(QRDetailViewModel.class);

        binding.toolbar.setNavigationContentDescription(R.string.navigate_up);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.btnCopy.setOnClickListener(view -> copyContent());
        binding.btnShare.setOnClickListener(view -> {
            String content = viewModel.getItem().getValue() == null ? "" : viewModel.getItem().getValue().getContent();
            ShareUtil.showShareChooser(this, content, viewModel.getBitmap().getValue());
        });
        binding.btnDelete.setOnClickListener(view -> confirmDelete());

        observeState();
        if (historyId == -1) finish();
        else viewModel.load(historyId);
    }

    private void observeState() {
        viewModel.getItem().observe(this, item -> {
            if (item == null) return;
            String sourceLabel = item.getSource() == HistorySource.GENERATED
                    ? getString(R.string.history_source_generated) : getString(R.string.history_source_scanned);
            binding.textTime.setText(dateFormat.format(item.getTimestamp()) + " • " + sourceLabel);
            binding.textContent.setText(item.getContent());
        });
        viewModel.getBitmap().observe(this, bitmap -> {
            if (bitmap != null) binding.imageQr.setImageBitmap(bitmap);
        });
        viewModel.getParsedContent().observe(this, parsed -> {
            QRActionBinder.bind(this, binding.layoutQrActions, parsed);
            if (parsed != null) {
                binding.textTypeChip.setText(QRTypeStyle.labelRes(parsed.getType()));
                binding.textTypeChip.setTextColor(QRTypeStyle.color(this, parsed.getType()));
                binding.textTypeChip.setBackgroundTintList(ColorStateList.valueOf(QRTypeStyle.softColor(this, parsed.getType())));
            }
        });
        viewModel.getDeleted().observe(this, deleted -> {
            if (Boolean.TRUE.equals(deleted)) finish();
        });
    }

    private void copyContent() {
        if (viewModel.getItem().getValue() == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.result_title), viewModel.getItem().getValue().getContent()));
        Snackbar.make(binding.getRoot(), R.string.copied, Snackbar.LENGTH_LONG).show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.cancel_action, null)
                .setPositiveButton(R.string.delete_action, (dialog, which) -> viewModel.delete())
                .show();
    }
}
