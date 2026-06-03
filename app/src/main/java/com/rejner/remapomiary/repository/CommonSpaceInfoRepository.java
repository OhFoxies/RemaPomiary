package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.CommonSpaceInfoDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.CommonSpaceInfo;

import java.util.List;
import java.util.function.Consumer;

public class CommonSpaceInfoRepository {

    private final CommonSpaceInfoDao dao;

    public CommonSpaceInfoRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.commonSpaceInfoDao(); // Add this method to AppDatabase.java
    }

    public void insert(CommonSpaceInfo info) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(info));
    }

    public void update(CommonSpaceInfo info) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(info));
    }

    public void delete(CommonSpaceInfo info) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(info));
    }

    public LiveData<List<CommonSpaceInfo>> getAll() {
        return dao.getAll();
    }

    public void getAllInfo(Consumer<List<CommonSpaceInfo>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<CommonSpaceInfo> result = dao.getAllSync();
            if (callback != null) {
                callback.accept(result);
            }
        });
    }

    public LiveData<List<CommonSpaceInfo>> getInfoByBlockId(int blockId) {
        return dao.getInfoByBlockId(blockId);
    }

    public void getInfoByBlockId(int blockId, Consumer<CommonSpaceInfo> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            CommonSpaceInfo result = dao.getInfoByBlockIdSync(blockId);
            if (callback != null) {
                callback.accept(result);
            }
        });
    }

    public LiveData<Boolean> checkIfExistsLive(int blockId) {
        return dao.checkIfExistsLive(blockId);
    }

    public boolean areAllFieldsFilledSync(int blockId) {
        return dao.areAllFieldsFilledSync(blockId);
    }

    public void areAllFieldsFilled(int blockId, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean result = dao.areAllFieldsFilledSync(blockId);
            if (callback != null) {
                callback.accept(result);
            }
        });
    }
}