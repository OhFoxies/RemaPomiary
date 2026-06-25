package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Contractors;
import com.rejner.remapomiary.repository.ContractorsRepository;

import java.util.List;

public class ContractorsViewModel extends AndroidViewModel {

    private final ContractorsRepository repository;
    private final LiveData<List<Contractors>> allContractors;

    public ContractorsViewModel(@NonNull Application application) {
        super(application);
        repository = new ContractorsRepository(application);
        allContractors = repository.getAllContractors();
    }

    public void insert(Contractors contractor) {
        repository.insert(contractor);
    }

    public void update(Contractors contractor) {
        repository.update(contractor);
    }

    public void delete(Contractors contractor) {
        repository.delete(contractor);
    }

    public void setActive(Contractors contractor) {
        repository.setActive(contractor);
    }

    public void setDefault(Contractors contractor) {
        repository.setDefault(contractor);
    }

    public void deactivateAll(int type) {
        repository.deactivateAll(type);
    }

    public void deactivateAllDefaults(int type) {
        repository.deactivateAllDefaults(type);
    }

    public LiveData<List<Contractors>> getAllContractors() {
        return allContractors;
    }

    public LiveData<List<Contractors>> getContractorsByType(int type) {
        return repository.getContractorsByType(type);
    }
}
