package com.example.qrapp.ui.generator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class QRFormGeneratorActivity extends BaseActivity {
    private ActivityQrFormGeneratorBinding binding;
    private QRGeneratorViewModel viewModel;
    private ActivityResultLauncher<String> permissionLauncher;
    private String selectedType = null;

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

        setupChipListeners();
        binding.btnGenerate.setOnClickListener(view -> generateFromForm());
        binding.btnSave.setOnClickListener(view -> saveWithPermission());
        binding.btnShare.setOnClickListener(view -> ShareUtil.showShareChooser(this,
                viewModel.getGeneratedContent(), viewModel.getCurrentBitmap()));
        observeState();
    }

    private void setupChipListeners() {
        LinearLayout[] forms = {binding.formWifi, binding.formContact, binding.formEmail,
                binding.formPhone, binding.formSms, binding.formUrl};
        String[] types = {"wifi", "contact", "email", "phone", "sms", "url"};
        int[] chipIds = {R.id.chip_wifi, R.id.chip_contact, R.id.chip_email,
                R.id.chip_phone, R.id.chip_sms, R.id.chip_url};

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            for (LinearLayout form : forms) form.setVisibility(View.GONE);
            
            // clear preview and actions
            binding.imageQr.setVisibility(View.GONE);
            binding.emptyPreview.setVisibility(View.VISIBLE);
            binding.btnSave.setEnabled(false);
            binding.btnShare.setEnabled(false);
            binding.layoutQrActions.setVisibility(View.GONE);
            
            // clear inputs
            binding.editSsid.setText("");
            binding.editWifiPassword.setText("");
            binding.editContactName.setText("");
            binding.editContactPhone.setText("");
            binding.editContactEmail.setText("");
            binding.editEmailAddress.setText("");
            binding.editEmailSubject.setText("");
            binding.editEmailBody.setText("");
            binding.editPhoneNumber.setText("");
            binding.editSmsPhone.setText("");
            binding.editSmsBody.setText("");
            binding.editUrl.setText("");

            if (checkedIds.isEmpty()) {
                selectedType = null;
                binding.btnGenerate.setEnabled(false);
                return;
            }
            int checkedId = checkedIds.get(0);
            for (int i = 0; i < chipIds.length; i++) {
                if (checkedId == chipIds[i]) {
                    forms[i].setVisibility(View.VISIBLE);
                    selectedType = types[i];
                    binding.btnGenerate.setEnabled(true);
                    break;
                }
            }
        });

        // Đánh dấu QR là cũ mỗi khi input thay đổi
        TextWatcher staleWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                viewModel.onInputChanged();
            }
        };
        TextInputEditText[] allInputs = {
                binding.editSsid, binding.editWifiPassword,
                binding.editContactName, binding.editContactPhone, binding.editContactEmail,
                binding.editEmailAddress, binding.editEmailSubject, binding.editEmailBody,
                binding.editPhoneNumber, binding.editSmsPhone, binding.editSmsBody,
                binding.editUrl
        };
        for (TextInputEditText input : allInputs) {
            input.addTextChangedListener(staleWatcher);
        }
    }

    private String formatContent() {
        if (selectedType == null) return "";
        switch (selectedType) {
            case "wifi":
                return formatWifi();
            case "contact":
                return formatContact();
            case "email":
                return formatEmail();
            case "phone":
                return formatPhone();
            case "sms":
                return formatSms();
            case "url":
                return formatUrl();
            default:
                return "";
        }
    }

    private String formatWifi() {
        String ssid = getText(binding.editSsid);
        String password = getText(binding.editWifiPassword);
        String security = "WPA";
        if (binding.chipWep.isChecked()) security = "WEP";
        else if (binding.chipNopass.isChecked()) security = "nopass";
        return "WIFI:T:" + security + ";S:" + escapeWifi(ssid) + ";P:" + escapeWifi(password) + ";;";
    }

    private String formatContact() {
        String name = getText(binding.editContactName);
        String phone = getText(binding.editContactPhone);
        String email = getText(binding.editContactEmail);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\nVERSION:3.0\n");
        if (!name.isEmpty()) sb.append("FN:").append(name).append("\n");
        if (!phone.isEmpty()) sb.append("TEL:").append(phone).append("\n");
        if (!email.isEmpty()) sb.append("EMAIL:").append(email).append("\n");
        sb.append("END:VCARD");
        return sb.toString();
    }

    private String formatEmail() {
        String email = getText(binding.editEmailAddress);
        String subject = getText(binding.editEmailSubject);
        String body = getText(binding.editEmailBody);
        StringBuilder sb = new StringBuilder("mailto:").append(email);
        boolean hasParams = false;
        if (!subject.isEmpty()) {
            sb.append("?subject=").append(urlEncode(subject));
            hasParams = true;
        }
        if (!body.isEmpty()) {
            sb.append(hasParams ? "&" : "?").append("body=").append(urlEncode(body));
        }
        return sb.toString();
    }

    private String formatPhone() {
        return "tel:" + getText(binding.editPhoneNumber);
    }

    private String formatSms() {
        String phone = getText(binding.editSmsPhone);
        String body = getText(binding.editSmsBody);
        String result = "smsto:" + phone;
        if (!body.isEmpty()) result += "?body=" + urlEncode(body);
        return result;
    }

    private String formatUrl() {
        String url = getText(binding.editUrl);
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }

    private void generateFromForm() {
        String content = formatContent();
        if (content.isEmpty() || !validateForm()) return;
        viewModel.generateQRCode(content);
    }

    private boolean validateForm() {
        if (selectedType == null) return false;
        switch (selectedType) {
            case "wifi":
                if (getText(binding.editSsid).isEmpty()) {
                    showMessage(getString(R.string.form_error_ssid));
                    return false;
                }
                return true;
            case "contact":
                if (getText(binding.editContactName).isEmpty() && getText(binding.editContactPhone).isEmpty()) {
                    showMessage(getString(R.string.form_error_name));
                    return false;
                }
                return true;
            case "email":
                String emailStr = getText(binding.editEmailAddress);
                if (emailStr.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
                    showMessage("Email không hợp lệ");
                    return false;
                }
                return true;
            case "phone":
                if (getText(binding.editPhoneNumber).isEmpty()) {
                    showMessage(getString(R.string.form_error_phone));
                    return false;
                }
                return true;
            case "sms":
                if (getText(binding.editSmsPhone).isEmpty()) {
                    showMessage(getString(R.string.form_error_sms_phone));
                    return false;
                }
                return true;
            case "url":
                String urlStr = getText(binding.editUrl);
                if (urlStr.isEmpty() || !android.util.Patterns.WEB_URL.matcher(urlStr).matches()) {
                    showMessage("Liên kết không hợp lệ");
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    private void observeState() {
        viewModel.getQrBitmap().observe(this, bitmap -> {
            binding.imageQr.setImageBitmap(bitmap);
            binding.imageQr.setVisibility(View.VISIBLE);
            binding.emptyPreview.setVisibility(View.GONE);
            binding.btnSave.setEnabled(true);
            binding.btnShare.setEnabled(Boolean.TRUE.equals(viewModel.getQrUpToDate().getValue()));
        });
        viewModel.getQrUpToDate().observe(this, upToDate -> {
            boolean hasBitmap = viewModel.getCurrentBitmap() != null;
            binding.btnShare.setEnabled(Boolean.TRUE.equals(upToDate) && hasBitmap);
        });
        viewModel.getParsedContent().observe(this, parsed -> QRActionBinder.bind(this, binding.layoutQrActions, parsed));
        viewModel.getSavedUri().observe(this, uri -> showMessage(getString(R.string.saved_success)));
        viewModel.getError().observe(this, this::showMessage);
        viewModel.getLoading().observe(this, loading -> {
            boolean active = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(active ? View.VISIBLE : View.GONE);
            binding.btnGenerate.setEnabled(!active && selectedType != null);
            binding.btnSave.setEnabled(!active && viewModel.getQrBitmap().getValue() != null);
            binding.btnShare.setEnabled(!active && Boolean.TRUE.equals(viewModel.getQrUpToDate().getValue())
                    && viewModel.getCurrentBitmap() != null);
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
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).setAnchorView(binding.btnGenerate).show();
    }

    private static String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private static String escapeWifi(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:");
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
