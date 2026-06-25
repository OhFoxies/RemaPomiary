package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.ContractorsDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Contractors;

import java.util.List;

public class ContractorsRepository {

    private final ContractorsDao dao;

    public ContractorsRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.contractorsDao(); // Remember to add this abstract method to AppDatabase.java
    }

    public void insert(Contractors contractor) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(contractor));
    }

    public void update(Contractors contractor) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(contractor));
    }

    public void delete(Contractors contractor) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(contractor));
    }

    public void setActive(Contractors contractor) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.deactivateAllByType(contractor.type);
            contractor.isActive = true;
            dao.update(contractor);
        });
    }

    public void setDefault(Contractors contractor) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.deactivateAllDefaultsByType(contractor.type);
            contractor.isDefault = true;
            dao.update(contractor);
        });
    }

    public void deactivateAll(int type) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.deactivateAllByType(type));
    }

    public void deactivateAllDefaults(int type) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.deactivateAllDefaultsByType(type));
    }

    public LiveData<List<Contractors>> getAllContractors() {
        return dao.getAllContractors();
    }

    public LiveData<List<Contractors>> getContractorsByType(int type) {
        return dao.getContractorsByType(type);
    }
}
