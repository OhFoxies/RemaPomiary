package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.CommonSpaceInfo;
import com.rejner.remapomiary.repository.CommonSpaceInfoRepository;

import java.util.List;
import java.util.function.Consumer;

public class CommonSpaceInfoViewModel extends AndroidViewModel {

    private final CommonSpaceInfoRepository repository;
    private final LiveData<List<CommonSpaceInfo>> allInfo;

    public CommonSpaceInfoViewModel(@NonNull Application application) {
        super(application);
        repository = new CommonSpaceInfoRepository(application);
        allInfo = repository.getAll();
    }

    public void insert(CommonSpaceInfo info) {
        repository.insert(info);
    }

    public void update(CommonSpaceInfo info) {
        repository.update(info);
    }

    public void delete(CommonSpaceInfo info) {
        repository.delete(info);
    }

    public LiveData<List<CommonSpaceInfo>> getAllInfo() {
        return allInfo;
    }

    /**
     * Asynchronous fetch for all CommonSpaceInfo, returning result via callback.
     */
    public void getAllInfo(Consumer<List<CommonSpaceInfo>> callback) {
        repository.getAllInfo(callback);
    }

    public LiveData<List<CommonSpaceInfo>> getInfoByBlockId(int blockId) {
        return repository.getInfoByBlockId(blockId);
    }

    /**
     * Asynchronous fetch for CommonSpaceInfo by blockId, returning result via callback.
     */
    public void getInfoByBlockId(int blockId, Consumer<CommonSpaceInfo> callback) {
        repository.getInfoByBlockId(blockId, callback);
    }

    // Use this to observe true/false in your Fragment or Activity
    public LiveData<Boolean> checkIfExists(int blockId) {
        return repository.checkIfExistsLive(blockId);
    }

    /**
     * Checks if all required fields are filled for a given block.
     * This method is synchronous and should be called from a background thread.
     */
    public boolean areAllFieldsFilledSync(int blockId) {
        return repository.areAllFieldsFilledSync(blockId);
    }

    /**
     * Asynchronous check for all fields filled, returning result via callback.
     */
    public void areAllFieldsFilled(int blockId, Consumer<Boolean> callback) {
        repository.areAllFieldsFilled(blockId, callback);
    }
}
