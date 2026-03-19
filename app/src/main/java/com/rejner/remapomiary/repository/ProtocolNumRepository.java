package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.ProtocolNumberDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.ProtocolNumber;

public class ProtocolNumRepository {
    private final ProtocolNumberDao dao;

    public ProtocolNumRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.protocolNumberDao();
    }
    public LiveData<Integer> getLastNumber() {
        return dao.getLastNumber();
    }


    public void update(ProtocolNumber pn) {
        AppDatabase.databaseWriteExecutor.execute(()-> {
            dao.update(pn);
        });
    }


    public void deleteOld() {
        AppDatabase.databaseWriteExecutor.execute(dao::deleteOld);
    }


    public void insert(ProtocolNumber pn) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insert(pn);
        });
    }
    public void saveLast() {
        AppDatabase.databaseWriteExecutor.execute(dao::saveLast);
    }


    public void updateCurrent(int number) {
        AppDatabase.databaseWriteExecutor.execute(()-> {
            dao.updateCurrent(number);
        });
    }


    public LiveData<Integer> getCurrentNumber() {
        return dao.getCurrentNumberLiveData();
    }
}
