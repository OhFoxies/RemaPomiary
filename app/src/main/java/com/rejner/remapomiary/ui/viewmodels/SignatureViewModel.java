package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Signature;
import com.rejner.remapomiary.repository.SignatureRepository;

public class SignatureViewModel extends AndroidViewModel {
    private final SignatureRepository repository;

    public SignatureViewModel(@NonNull Application application) {
        super(application);
        repository = new SignatureRepository(application);
    }

    public void insert(Signature signature) {
        repository.insert(signature);
    }

    public LiveData<Signature> getSignatureForFlat(int flatId) {
        return repository.getSignatureForFlat(flatId);
    }
    public void deleteSignatureForFlat(int flatId) {
        repository.deleteSignatureForFlat(flatId);
    }
}