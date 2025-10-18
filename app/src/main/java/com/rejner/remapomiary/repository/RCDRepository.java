package com.rejner.remapomiary.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.rejner.remapomiary.data.dao.RCDDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.RCD;

import java.util.List;

public class RCDRepository {

    private final RCDDao dao;

    public RCDRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.rcdDao();
    }

    public void insert(RCD rcd) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(rcd));
    }

    public void update(RCD rcd) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(rcd));
    }

    public void delete(RCD rcd) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(rcd));
    }

    public LiveData<List<RCD>> getRcdsForFlat(int flatId) {
        return dao.getRcdsForFlat(flatId);
    }
}
