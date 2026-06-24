package com.rejner.remapomiary.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.rejner.remapomiary.data.dao.FlatPhotoDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.FlatPhoto;
import java.util.List;

public class FlatPhotoRepository {
    private final FlatPhotoDao dao;

    public FlatPhotoRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.flatPhotoDao(); // Pamiętaj o dodaniu metody 'public abstract FlatPhotoDao flatPhotoDao();' w klasie AppDatabase
    }

    public void insert(FlatPhoto photo) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(photo));
    }

    public void delete(FlatPhoto photo) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(photo));
    }

    public LiveData<List<FlatPhoto>> getPhotosByFlatAndType(int flatId, int type) {
        return dao.getPhotosByFlatAndType(flatId, type);
    }

    public void update(FlatPhoto photo) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(photo));
    }


}