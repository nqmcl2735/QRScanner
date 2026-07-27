package com.example.qrapp.ui.scanner;

import com.example.qrapp.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CameraCaptureActivity extends CaptureActivity {
    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_camera_capture);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationContentDescription(R.string.navigate_up);
        toolbar.setNavigationOnClickListener(view -> finish());

        return findViewById(R.id.zxing_barcode_scanner);
    }
}
