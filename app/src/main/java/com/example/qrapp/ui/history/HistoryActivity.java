package com.example.qrapp.ui.history;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.qrapp.R;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.databinding.ActivityHistoryBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.detail.QRDetailActivity;
import com.example.qrapp.ui.viewmodel.ViewModelFactory;
import com.example.qrapp.util.FileProviderUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.util.Log;

public class HistoryActivity extends BaseActivity implements HistoryAdapter.Listener {
    private ActivityHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private String pendingExportCsv = null;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.toolbar);

        HistoryRepository repository = new HistoryRepository(this, new HistorySqliteDataSource(this));
        viewModel = new ViewModelProvider(this, ViewModelFactory.forHistory(repository)).get(HistoryViewModel.class);

        adapter = new HistoryAdapter(this);
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHistory.setAdapter(adapter);

        binding.toolbar.setNavigationContentDescription(R.string.navigate_up);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.toolbar.inflateMenu(R.menu.menu_history);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export) {
                viewModel.exportHistory();
                return true;
            } else if (id == R.id.action_import) {
                importLauncher.launch(new String[]{"text/comma-separated-values", "text/csv", "*/*"});
                return true;
            }
            return false;
        });

        setupLaunchers();
        observeState();

    }

    @Override protected void onResume() {
        super.onResume();
        viewModel.loadHistory();
    }

    private void setupLaunchers() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/csv"),
                uri -> {
                    if (uri != null && pendingExportCsv != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            if (os != null) {
                                os.write(pendingExportCsv.getBytes());
                                showMessage(getString(R.string.export_success, pendingExportCsv.split("\n").length - 1));
                            }
                        } catch (Exception e) {
                            Log.e("HistoryActivity", "Export error", e);
                            showMessage(getString(R.string.export_error));
                        }
                    } else if (uri == null) {
                        // User cancelled
                    } else {
                        showMessage(getString(R.string.export_error));
                    }
                    pendingExportCsv = null;
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try (InputStream is = getContentResolver().openInputStream(uri);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            viewModel.importHistory(sb.toString());
                        } catch (Exception e) {
                            Log.e("HistoryActivity", "Import error", e);
                            showMessage(getString(R.string.import_error));
                        }
                    }
                });
    }

    private void observeState() {
        viewModel.getItems().observe(this, items -> {
            adapter.submitList(items);
            boolean empty = items == null || items.isEmpty();
            binding.emptyHistory.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.getLoading().observe(this, loading -> binding.progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        
        viewModel.getExportResult().observe(this, csv -> {
            if (csv != null && !csv.isEmpty() && csv.split("\n").length > 1) {
                pendingExportCsv = csv;
                exportLauncher.launch(getString(R.string.export_file_name) + ".csv");
            } else {
                showMessage(getString(R.string.export_empty));
            }
        });

        viewModel.getImportResult().observe(this, count -> {
            if (count != null) {
                if (count > 0) {
                    showMessage(getString(R.string.import_success, count));
                } else {
                    showMessage(getString(R.string.import_error));
                }
            }
        });
    }


    @Override
    public void onItemClick(QRHistoryItem item) {
        Intent intent = new Intent(this, QRDetailActivity.class);
        intent.putExtra(QRDetailActivity.EXTRA_HISTORY_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onCopyText(QRHistoryItem item) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.result_title), item.getContent()));
        showMessage(getString(R.string.copied));
    }

    @Override
    public void onCopyImage(QRHistoryItem item) {
        if (item.getImagePath() == null) {
            showMessage(getString(R.string.copy_image_error));
            return;
        }
        Uri uri = FileProviderUtil.getUriForFile(this, new File(item.getImagePath()));
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), getString(R.string.qr_preview), uri));
        showMessage(getString(R.string.copied_image));
    }

    @Override
    public void onDelete(QRHistoryItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.cancel_action, null)
                .setPositiveButton(R.string.delete_action, (dialog, which) -> {
                    viewModel.deleteItem(item);
                    showMessage(getString(R.string.deleted_success));
                })
                .show();
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
