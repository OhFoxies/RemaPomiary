package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.repository.OutletMeasurementRepository;

import java.util.List;

public class OutletMeasurementViewModel extends AndroidViewModel {

    public final OutletMeasurementRepository repository;

    public OutletMeasurementViewModel(@NonNull Application application) {
        super(application);
        repository = new OutletMeasurementRepository(application);
    }
    public interface outletCallback {
        void onResult(long lastId);
    }

    public void insert(OutletMeasurement outletMeasurement, OutletMeasurementViewModel.outletCallback callback) {
        repository.insert(outletMeasurement, callback::onResult);
    }

    public void update(OutletMeasurement outletMeasurement, Runnable onFinished) {
        repository.update(outletMeasurement, onFinished);
    }

    public void delete(OutletMeasurement outletMeasurement, Runnable onFinished) {
        repository.delete(outletMeasurement, onFinished);
    }

    public LiveData<List<OutletMeasurement>> getMeasurementsForRoom(int roomId) {
        return repository.getMeasurementsForRoom(roomId);
    }
}
