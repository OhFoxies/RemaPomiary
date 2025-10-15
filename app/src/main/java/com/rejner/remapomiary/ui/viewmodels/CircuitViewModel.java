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

    // Pobranie listy obwodów w danym mieszkaniu
    public LiveData<List<Circuit>> getCircuitsForFlat(int flatId) {
        return repository.getCircuitsForFlat(flatId);
    }

    // Dodanie obwodu
    public void insert(Circuit circuit) {
        repository.insert(circuit);
    }

    // Aktualizacja obwodu
    public void update(Circuit circuit) {
        repository.update(circuit);
    }

    // Usunięcie obwodu
    public void delete(Circuit circuit) {
        repository.delete(circuit);
    }
}
