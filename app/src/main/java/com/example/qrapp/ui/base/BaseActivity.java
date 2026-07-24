package com.example.qrapp.ui.base;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    protected void applySystemBars(View root, @Nullable View topView) {
        final int rootLeft = root.getPaddingLeft();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();
        final int topPadding = topView == null ? 0 : topView.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(rootLeft + bars.left, view.getPaddingTop(), rootRight + bars.right, rootBottom + bars.bottom);
            if (topView != null) topView.setPadding(topView.getPaddingLeft(), topPadding + bars.top, topView.getPaddingRight(), topView.getPaddingBottom());
            else view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), view.getPaddingBottom());
            return windowInsets;
        });
    }
}
