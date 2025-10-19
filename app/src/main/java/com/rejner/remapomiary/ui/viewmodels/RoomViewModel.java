package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.RoomFullData;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.repository.RoomRepository;

import java.util.List;

public class RoomViewModel extends AndroidViewModel {

    private final RoomRepository repository;

    public RoomViewModel(@NonNull Application application) {
        super(application);
        repository = new RoomRepository(application);
    }

    public interface RoomCallback {
        void onResult(RoomFullData roomData);
    }

    public interface idCallBack {
        void onResult(Long id);
    }

    public void insertWithId(RoomInFlat room, idCallBack callBack) {
        repository.insertWithId(room, callBack::onResult);
    }
    // Zwraca listę pokoi w danym mieszkaniu
    public LiveData<List<RoomInFlat>> getRoomsForFlat(int flatId) {
        return repository.getRoomsForFlat(flatId);
    }

    // Zwraca pełne dane pokoju (LiveData)
    public LiveData<RoomFullData> getRoomFullData(int roomId) {
        return repository.getRoomFullData(roomId);
    }

    // Dodanie pokoju
    public void insert(RoomInFlat room) {
        repository.insert(room);
    }

    // Aktualizacja pokoju
    public void update(RoomInFlat room) {
        repository.update(room);
    }

    // Usunięcie pokoju
    public void delete(RoomInFlat room) {
        repository.delete(room);
    }
}
