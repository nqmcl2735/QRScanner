package com.example.qrapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.qrapp.databinding.ActivityHomeBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.generator.QRFormGeneratorActivity;
import com.example.qrapp.ui.generator.QRGeneratorActivity;
import com.example.qrapp.ui.history.HistoryActivity;
import com.example.qrapp.ui.scanner.CameraScannerActivity;
import com.example.qrapp.ui.scanner.QRScannerActivity;

public class HomeActivity extends BaseActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.heroHeader);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        insetsController.setAppearanceLightStatusBars(false);
        binding.cardGenerate.setOnClickListener(view -> startActivity(new Intent(this, QRGeneratorActivity.class)));
        binding.cardCameraScan.setOnClickListener(view -> startActivity(new Intent(this, CameraScannerActivity.class)));
        binding.cardFormGenerate.setOnClickListener(view -> startActivity(new Intent(this, QRFormGeneratorActivity.class)));
        binding.cardScan.setOnClickListener(view -> startActivity(new Intent(this, QRScannerActivity.class)));
        binding.cardHistory.setOnClickListener(view -> startActivity(new Intent(this, HistoryActivity.class)));
    }
}
