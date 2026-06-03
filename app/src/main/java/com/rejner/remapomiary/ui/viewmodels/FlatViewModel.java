package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.repository.FlatRepository;

import java.util.List;
import java.util.function.Consumer;

public class FlatViewModel extends AndroidViewModel {
    private final FlatRepository repository;

    public FlatViewModel(@NonNull Application application) {
        super(application);
        repository = new FlatRepository(application);
    }

    public interface FlatCallback {
        void onResult(FlatFullData flat);
    }

    public interface FlatsCallback {
        void onResult(List<FlatFullData> flats);
    }

    public interface IdCallBack {
        void onResult(Long x);
    }
    public interface BooleanCallBack {
        void onResult(Boolean x);
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

    public void getFlatsSync(int blockId, FlatsCallback callback) {
        repository.getFlatsSync(blockId, callback::onResult);
    }


    public LiveData<List<Flat>> getFlatsByBlockId(int blockId) {
        return repository.getFlatsByBlockId(blockId);
    }

    public LiveData<List<FlatFullData>> getFlatsFullDataByBlockId(int blockId) {
        return repository.getFlatsFullDataByBlockId(blockId);
    }

    private final MediatorLiveData<Flat> flatMediator = new MediatorLiveData<>();

    public LiveData<Flat> getCombinedFlat(int flatId) {
        LiveData<Flat> flatLiveData = repository.getFlatById(flatId);
        LiveData<Boolean> verdictLiveData = shouldSetGradeToOne(flatId);

        flatMediator.addSource(flatLiveData, flat -> flatMediator.setValue(flat));

        flatMediator.addSource(verdictLiveData, verdict -> {
            Flat flat = flatMediator.getValue();
            if (flat == null) return;

            if (flat.gradeByUser == 0) {
                int newGrade = verdict ? 1 : 0;
                if (flat.grade != newGrade) {
                    flat.grade = newGrade;
                    update(flat);
                }
            }
        });


        return flatMediator;
    }

    public void insertWithId(Flat flat, IdCallBack callBack) {
        repository.insertWithId(flat, callBack::onResult);
    }

    public LiveData<List<FlatFullData>> getTemplatesForCatalog(int catalogId) {
        return repository.getTemplatesForCatalog(catalogId);
    }
    public void toggleGradeBlock(int flatId) {
        repository.getFlatByIdSync(flatId, flatFullData -> {
            if (flatFullData != null) {
                if (flatFullData.flat.gradeByUser == 0) {
                    flatFullData.flat.gradeByUser = 1;
                } else {
                    flatFullData.flat.gradeByUser = 0;
                }
                repository.update(flatFullData.flat);
            }
        });
    }

    public LiveData<Flat> getCommonSpace(int blockId) {
        return repository.getCommonSpace(blockId);
    }

    public void getCommonSpace(int blockId, Consumer<Flat> callback) {
        repository.getCommonSpace(blockId, callback);
    }


    private LiveData<Boolean> shouldSetGradeToOne(int flatId) {
        return repository.shouldSetGradeToOne(flatId);
    }

}
