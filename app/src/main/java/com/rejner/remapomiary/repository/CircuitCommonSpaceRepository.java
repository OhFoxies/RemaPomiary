package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.CircuitCommonSpaceDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;

import java.util.List;

public class CircuitCommonSpaceRepository {

    private final CircuitCommonSpaceDao dao;

    public CircuitCommonSpaceRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.circuitCommonSpaceDao(); // Make sure to add this method to your AppDatabase class
    }

    public void insert(CircuitCommonSpace circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(circuit));
    }

    public void update(CircuitCommonSpace circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(circuit));
    }

    public void delete(CircuitCommonSpace circuit) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(circuit));
    }

    public void deleteCircuitsByBoardId(int boardId) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.deleteCircuitsByBoardId(boardId));
    }

    public LiveData<List<CircuitCommonSpace>> getCircuitsForBoard(int boardId) {
        return dao.getCircuitsForBoard(boardId);
    }
}