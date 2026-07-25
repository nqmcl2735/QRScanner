package com.example.qrapp.ui.generator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
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
import com.example.qrapp.databinding.ActivityQrFormGeneratorBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.viewmodel.ViewModelFactory;
import com.example.qrapp.util.QRActionBinder;
import com.example.qrapp.util.ShareUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

public class QRFormGeneratorActivity extends BaseActivity {
    private ActivityQrFormGeneratorBinding binding;
    private QRGeneratorViewModel viewModel;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrFormGeneratorBinding.inflate(getLayoutInflater());
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
        
        setupChips();
        
        binding.btnGenerate.setOnClickListener(view -> generateQR());
        binding.btnSave.setOnClickListener(view -> saveWithPermission());
        binding.btnShare.setOnClickListener(view -> ShareUtil.showShareChooser(this,
                "", viewModel.getCurrentBitmap())); // No raw text provided, but QR data isn't needed by share intent's text.
        observeState();
        
        // Show wifi by default
        binding.chipWifi.setChecked(true);
        showForm(binding.formWifi);
    }

    private void setupChips() {
        binding.chipGroupTypes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            hideAllForms();
            if (checkedId == R.id.chip_wifi) showForm(binding.formWifi);
            else if (checkedId == R.id.chip_contact) showForm(binding.formContact);
            else if (checkedId == R.id.chip_email) showForm(binding.formEmail);
            else if (checkedId == R.id.chip_phone) showForm(binding.formPhone);
            else if (checkedId == R.id.chip_sms) showForm(binding.formSms);
            else if (checkedId == R.id.chip_url) showForm(binding.formUrl);
        });
    }
    
    private void hideAllForms() {
        binding.formWifi.setVisibility(View.GONE);
        binding.formContact.setVisibility(View.GONE);
        binding.formEmail.setVisibility(View.GONE);
        binding.formPhone.setVisibility(View.GONE);
        binding.formSms.setVisibility(View.GONE);
        binding.formUrl.setVisibility(View.GONE);
    }
    
    private void showForm(LinearLayout form) {
        form.setVisibility(View.VISIBLE);
    }
    
    private void generateQR() {
        String data = "";
        if (binding.formWifi.getVisibility() == View.VISIBLE) {
            String ssid = binding.editWifiSsid.getText().toString().trim();
            String pwd = binding.editWifiPassword.getText().toString().trim();
            if (TextUtils.isEmpty(ssid)) {
                showMessage(getString(R.string.form_error_ssid));
                return;
            }
            String sec = "WPA";
            int secId = binding.chipGroupSecurity.getCheckedChipId();
            if (secId == R.id.chip_security_wep) sec = "WEP";
            else if (secId == R.id.chip_security_none) sec = "nopass";
            
            data = "WIFI:T:" + sec + ";S:" + ssid + ";P:" + pwd + ";;";
        } else if (binding.formContact.getVisibility() == View.VISIBLE) {
            String name = binding.editContactName.getText().toString().trim();
            String phone = binding.editContactPhone.getText().toString().trim();
            String email = binding.editContactEmail.getText().toString().trim();
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(phone)) {
                showMessage(getString(R.string.form_error_name));
                return;
            }
            data = "BEGIN:VCARD\nVERSION:3.0\nFN:" + name + "\nTEL:" + phone + "\nEMAIL:" + email + "\nEND:VCARD";
        } else if (binding.formEmail.getVisibility() == View.VISIBLE) {
            String email = binding.editEmailAddress.getText().toString().trim();
            String subject = binding.editEmailSubject.getText().toString().trim();
            String body = binding.editEmailBody.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showMessage(getString(R.string.form_error_email));
                return;
            }
            data = "mailto:" + email + "?subject=" + android.net.Uri.encode(subject) + "&body=" + android.net.Uri.encode(body);
        } else if (binding.formPhone.getVisibility() == View.VISIBLE) {
            String phone = binding.editPhoneNumber.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                showMessage(getString(R.string.form_error_phone));
                return;
            }
            data = "tel:" + phone;
        } else if (binding.formSms.getVisibility() == View.VISIBLE) {
            String phone = binding.editSmsPhone.getText().toString().trim();
            String body = binding.editSmsBody.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                showMessage(getString(R.string.form_error_sms_phone));
                return;
            }
            data = "smsto:" + phone + "?body=" + android.net.Uri.encode(body);
        } else if (binding.formUrl.getVisibility() == View.VISIBLE) {
            String url = binding.editUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                showMessage(getString(R.string.form_error_url));
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            data = url;
        }
        
        if (!TextUtils.isEmpty(data)) {
            viewModel.generateQRCode(data);
        }
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
        viewModel.getError().observe(this, message -> showMessage(message));
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
