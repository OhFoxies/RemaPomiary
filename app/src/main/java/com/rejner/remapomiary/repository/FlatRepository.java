package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.FlatDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;

import java.util.List;
import java.util.function.Consumer;

public class FlatRepository {
    private final FlatDao dao;

    public FlatRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.flatDao();
    }

    public void insert(Flat flat) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(flat));
    }

    public void update(Flat flat) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(flat));
    }

    public void delete(Flat flat) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(flat));
    }

    public LiveData<Flat> getFlatById(int flatId) {
        return dao.getFlatById(flatId);
    }

    public LiveData<List<Flat>> getAllFlats() {
        return dao.getAllFlats();
    }

    public LiveData<FlatFullData> getFlatFullData(int flatId) {
        return dao.getFlatFullData(flatId);
    }

    public LiveData<List<FlatFullData>> getAllFlatsFullData() {
        return dao.getAllFlatsFullData();
    }

    public LiveData<List<Flat>> getFlatsByBlockId(int blockId) {
        return dao.getFlatsByBlockId(blockId);
    }

    public LiveData<List<FlatFullData>> getFlatsFullDataByBlockId(int blockId) {
        return dao.getFlatsFullDataByBlockId(blockId);
    }

    public void getFlatByIdSync(int flatId, Consumer<FlatFullData> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
             FlatFullData flat = dao.getFlatFullDataSync(flatId);
             callback.accept(flat);
        });
    }
}
