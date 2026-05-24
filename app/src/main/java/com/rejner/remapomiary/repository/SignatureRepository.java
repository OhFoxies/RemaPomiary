package com.rejner.remapomiary.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.SignatureDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Signature;

public class SignatureRepository {
    private final SignatureDao dao;

    public SignatureRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.signatureDao();
    }

    public void insert(Signature signature) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(signature));
    }

    public LiveData<Signature> getSignatureForFlat(int flatId) {
        return dao.getSignatureForFlat(flatId);
    }

    public void deleteSignatureForFlat(int flatId) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.deleteSignatureForFlat(flatId));
    }
}