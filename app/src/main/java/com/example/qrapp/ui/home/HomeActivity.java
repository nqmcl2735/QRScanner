package com.example.qrapp.ui.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.qrapp.R;
import com.example.qrapp.data.model.HistorySource;
import com.example.qrapp.data.model.QRHistoryItem;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.source.history.HistorySqliteDataSource;
import com.example.qrapp.databinding.ActivityHomeBinding;
import com.example.qrapp.ui.base.BaseActivity;
import com.example.qrapp.ui.detail.QRDetailActivity;
import com.example.qrapp.ui.generator.QRFormGeneratorActivity;
import com.example.qrapp.ui.generator.QRGeneratorActivity;
import com.example.qrapp.ui.history.HistoryActivity;
import com.example.qrapp.ui.scanner.CameraScannerActivity;
import com.example.qrapp.ui.scanner.QRScannerActivity;
import com.example.qrapp.util.QRTypeStyle;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends BaseActivity {
    private ActivityHomeBinding binding;
    private HistoryRepository historyRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBars(binding.getRoot(), binding.heroHeader);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        insetsController.setAppearanceLightStatusBars(false);
        binding.cardGenerate.setOnClickListener(view -> startActivity(new Intent(this, QRGeneratorActivity.class)));
        binding.cardCameraScan.setOnClickListener(view -> startActivity(new Intent(this, CameraScannerActivity.class)));
        binding.cardFormGenerate.setOnClickListener(view -> startActivity(new Intent(this, QRFormGeneratorActivity.class)));
        binding.cardScan.setOnClickListener(view -> startActivity(new Intent(this, QRScannerActivity.class)));
        binding.cardHistory.setOnClickListener(view -> startActivity(new Intent(this, HistoryActivity.class)));

        historyRepository = new HistoryRepository(this, new HistorySqliteDataSource(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void loadRecentHistory() {
        executor.execute(() -> {
            List<QRHistoryItem> items = historyRepository.getAll();
            runOnUiThread(() -> updateRecentUI(items));
        });
    }

    private void updateRecentUI(List<QRHistoryItem> items) {
        if (items == null || items.isEmpty()) {
            binding.layoutRecentSection.setVisibility(View.GONE);
            return;
        }

        binding.layoutRecentSection.setVisibility(View.VISIBLE);
        binding.layoutRecentItems.removeAllViews();

        int maxItems = Math.min(items.size(), 2);
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < maxItems; i++) {
            QRHistoryItem item = items.get(i);
            View itemView = inflater.inflate(R.layout.item_home_recent, binding.layoutRecentItems, false);

            TextView textContent = itemView.findViewById(R.id.text_content);
            TextView textSubtitle = itemView.findViewById(R.id.text_subtitle);
            FrameLayout iconBg = itemView.findViewById(R.id.icon_bg);
            ImageView iconImage = itemView.findViewById(R.id.icon_image);

            textContent.setText(item.getContent());

            String sourceStr = item.getSource() == HistorySource.GENERATED ? "Đã tạo" : "Đã quét";
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            textSubtitle.setText(sourceStr + " • " + relativeTime);

            int softColor = QRTypeStyle.softColor(this, item.getType());
            int color = QRTypeStyle.color(this, item.getType());
            iconBg.setBackgroundTintList(ColorStateList.valueOf(softColor));
            iconImage.setImageResource(QRTypeStyle.iconRes(item.getType()));
            iconImage.setImageTintList(ColorStateList.valueOf(color));

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(this, QRDetailActivity.class);
                intent.putExtra(QRDetailActivity.EXTRA_HISTORY_ID, item.getId());
                startActivity(intent);
            });

            binding.layoutRecentItems.addView(itemView);

            if (i < maxItems - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (int) (0.5f * getResources().getDisplayMetrics().density));
                params.setMarginStart((int) (62 * getResources().getDisplayMetrics().density));
                divider.setLayoutParams(params);
                divider.setBackgroundColor(getColor(R.color.outline));
                binding.layoutRecentItems.addView(divider);
            }
        }
    }
}
