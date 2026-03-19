package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.repository.CircuitRepository;

import java.util.List;

public class CircuitViewModel extends AndroidViewModel {

    private final CircuitRepository repository;

    public CircuitViewModel(@NonNull Application application) {
        super(application);
        repository = new CircuitRepository(application);
    }

    public LiveData<List<Circuit>> getCircuitsForFlat(int flatId) {
        return repository.getCircuitsForFlat(flatId);
    }

    public void insert(Circuit circuit) {
        repository.insert(circuit);
    }

    public void update(Circuit circuit) {
        repository.update(circuit);
    }

    public void delete(Circuit circuit) {
        repository.delete(circuit);
    }
}
