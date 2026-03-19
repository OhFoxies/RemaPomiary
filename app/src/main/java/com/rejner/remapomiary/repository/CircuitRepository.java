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

    public void insert(Circuit circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(circuit));
    }

    public void update(Circuit circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(circuit));
    }

    public void delete(Circuit circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(circuit));
    }

    public LiveData<List<Circuit>> getCircuitsForFlat(int flatId) {
        return dao.getCircuitsForFlat(flatId);
    }
}
