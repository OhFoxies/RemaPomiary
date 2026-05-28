package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.CommonSpaceInfo;
import com.rejner.remapomiary.repository.CommonSpaceInfoRepository;

import java.util.List;

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

    public LiveData<List<CommonSpaceInfo>> getInfoByBlockId(int blockId) {
        return repository.getInfoByBlockId(blockId);
    }

    // Use this to observe true/false in your Fragment or Activity
    public LiveData<Boolean> checkIfExists(int blockId) {
        return repository.checkIfExistsLive(blockId);
    }
}