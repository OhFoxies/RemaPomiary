package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.CommonSpaceInfoDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.CommonSpaceInfo;

import java.util.List;

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

    public LiveData<List<CommonSpaceInfo>> getInfoByBlockId(int blockId) {
        return dao.getInfoByBlockId(blockId);
    }

    public LiveData<Boolean> checkIfExistsLive(int blockId) {
        return dao.checkIfExistsLive(blockId);
    }
}