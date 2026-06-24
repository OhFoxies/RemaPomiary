package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.rejner.remapomiary.data.entities.FlatPhoto;
import com.rejner.remapomiary.repository.FlatPhotoRepository;
import java.util.List;

public class FlatPhotoViewModel extends AndroidViewModel {
    private final FlatPhotoRepository repository;

    public FlatPhotoViewModel(@NonNull Application application) {
        super(application);
        repository = new FlatPhotoRepository(application);
    }

    public void insert(FlatPhoto photo) {
        repository.insert(photo);
    }

    public void delete(FlatPhoto photo) {
        repository.delete(photo);
    }

    public LiveData<List<FlatPhoto>> getPhotosByFlatAndType(int flatId, int type) {
        return repository.getPhotosByFlatAndType(flatId, type);
    }

    public void update(FlatPhoto photo) {
        repository.update(photo);
    }

}