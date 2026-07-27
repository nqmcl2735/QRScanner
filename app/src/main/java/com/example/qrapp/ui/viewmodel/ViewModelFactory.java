package com.example.qrapp.ui.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.qrapp.data.repository.HistoryRepository;
import com.example.qrapp.data.repository.QRGeneratorRepository;
import com.example.qrapp.data.repository.QRScannerRepository;
import com.example.qrapp.ui.detail.QRDetailViewModel;
import com.example.qrapp.ui.generator.QRGeneratorViewModel;
import com.example.qrapp.ui.history.HistoryViewModel;
import com.example.qrapp.ui.scanner.CameraScannerViewModel;
import com.example.qrapp.ui.scanner.QRScannerViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {
    @Nullable private final QRGeneratorRepository generatorRepository;
    @Nullable private final QRScannerRepository scannerRepository;
    @Nullable private final HistoryRepository historyRepository;

    private ViewModelFactory(@Nullable QRGeneratorRepository generatorRepository,
                              @Nullable QRScannerRepository scannerRepository,
                              @Nullable HistoryRepository historyRepository) {
        this.generatorRepository = generatorRepository;
        this.scannerRepository = scannerRepository;
        this.historyRepository = historyRepository;
    }

    public static ViewModelFactory forGenerator(QRGeneratorRepository repository, HistoryRepository historyRepository) {
        return new ViewModelFactory(repository, null, historyRepository);
    }

    public static ViewModelFactory forScanner(QRScannerRepository repository, HistoryRepository historyRepository) {
        return new ViewModelFactory(null, repository, historyRepository);
    }

    public static ViewModelFactory forCameraScanner(HistoryRepository historyRepository) {
        return new ViewModelFactory(null, null, historyRepository);
    }

    public static ViewModelFactory forHistory(HistoryRepository historyRepository) {
        return new ViewModelFactory(null, null, historyRepository);
    }

    @NonNull @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(QRGeneratorViewModel.class) && generatorRepository != null) {
            return (T) new QRGeneratorViewModel(generatorRepository, historyRepository);
        }
        if (modelClass.isAssignableFrom(QRScannerViewModel.class) && scannerRepository != null) {
            return (T) new QRScannerViewModel(scannerRepository, historyRepository);
        }
        if (modelClass.isAssignableFrom(CameraScannerViewModel.class) && historyRepository != null) {
            return (T) new CameraScannerViewModel(historyRepository);
        }
        if (modelClass.isAssignableFrom(HistoryViewModel.class) && historyRepository != null) {
            return (T) new HistoryViewModel(historyRepository);
        }
        if (modelClass.isAssignableFrom(QRDetailViewModel.class) && historyRepository != null) {
            return (T) new QRDetailViewModel(historyRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
