package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.ProtocolNumber;
import com.rejner.remapomiary.repository.ProtocolNumRepository;

public class ProtocolNumViewModel extends AndroidViewModel {
    public final ProtocolNumRepository repository;

    public ProtocolNumViewModel(@NonNull Application application) {
        super(application);
        repository = new ProtocolNumRepository(application);
    }

    public LiveData<Integer> getLastNumber() {
        return repository.getLastNumber();
    }

    public LiveData<Integer> getCurrentNumber() {
        return repository.getCurrentNumber();
    }

    public void update(ProtocolNumber pn) {
         repository.update(pn);
    }

    public void deleteOld() {
        repository.deleteOld();
    }

    public void insert(ProtocolNumber pn){
        repository.insert(pn);
    }

    public void saveLast() {
        repository.saveLast();
    }

    public void updateCurrent(int number) {
        repository.updateCurrent(number);
    }
}
