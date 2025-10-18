package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.repository.RCDRepository;

import java.util.List;

public class RCDViewModel extends AndroidViewModel {

    private RCDRepository repository;

    public RCDViewModel(Application application) {
        super(application);
        repository = new RCDRepository(application);
    }

    public void insert(RCD rcd) {
        repository.insert(rcd);
    }

    public void update(RCD rcd) {
        repository.update(rcd);
    }

    public void delete(RCD rcd) {
        repository.delete(rcd);
    }

    public LiveData<List<RCD>> getRcdsForFlat(int flatId) {
        return repository.getRcdsForFlat(flatId);
    }
}
