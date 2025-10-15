package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.CircuitDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Circuit;

import java.util.List;

public class CircuitRepository {

    private final CircuitDao dao;

    public CircuitRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.circuitDao();
    }

    // Dodanie obwodu
    public void insert(Circuit circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(circuit));
    }

    // Aktualizacja obwodu
    public void update(Circuit circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(circuit));
    }

    // Usunięcie obwodu
    public void delete(Circuit circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(circuit));
    }

    // Pobranie listy obwodów dla mieszkania
    public LiveData<List<Circuit>> getCircuitsForFlat(int flatId) {
        return dao.getCircuitsForFlat(flatId);
    }
}
