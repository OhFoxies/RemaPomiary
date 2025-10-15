package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.repository.FlatRepository;

import java.util.List;

public class FlatViewModel extends AndroidViewModel {
    private final FlatRepository repository;

    public FlatViewModel(@NonNull Application application) {
        super(application);
        repository = new FlatRepository(application);
    }

    public interface FlatCallback {
        void onResult(FlatFullData flat);
    }

    public LiveData<List<Flat>> getAllFlats() {
        return repository.getAllFlats();
    }

    public LiveData<List<FlatFullData>> getAllFlatsFullData() {
        return repository.getAllFlatsFullData();
    }

    public LiveData<Flat> getFlatById(int flatId) {
        return repository.getFlatById(flatId);
    }

    public LiveData<FlatFullData> getFlatFullData(int flatId) {
        return repository.getFlatFullData(flatId);
    }

    public void insert(Flat flat) {
        repository.insert(flat);
    }

    public void update(Flat flat) {
        repository.update(flat);
    }

    public void delete(Flat flat) {
        repository.delete(flat);
    }

    public void getFlatByIdSync(int flatId, FlatCallback callback) {
        repository.getFlatByIdSync(flatId, callback::onResult);
    }

    public LiveData<List<Flat>> getFlatsByBlockId(int blockId) {
        return repository.getFlatsByBlockId(blockId);
    }

    public LiveData<List<FlatFullData>> getFlatsFullDataByBlockId(int blockId) {
        return repository.getFlatsFullDataByBlockId(blockId);
    }
}
