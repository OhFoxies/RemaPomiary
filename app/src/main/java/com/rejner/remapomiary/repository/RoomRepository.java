package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.RoomDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.RoomFullData;
import com.rejner.remapomiary.data.entities.RoomInFlat;

import java.util.List;
import java.util.function.Consumer;

public class RoomRepository {

    private final RoomDao dao;

    public RoomRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.roomDao();
    }

    // Wstawianie pokoju
    public void insert(RoomInFlat room) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(room));
    }

    // Aktualizacja pokoju
    public void update(RoomInFlat room) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(room));
    }

    // Usuwanie pokoju
    public void delete(RoomInFlat room) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(room));
    }

    // Pobieranie listy pokoi dla danego mieszkania (LiveData)
    public LiveData<List<RoomInFlat>> getRoomsForFlat(int flatId) {
        return dao.getRoomsForFlat(flatId);
    }

    // Pobieranie pełnych danych pokoju
    public LiveData<RoomFullData> getRoomFullData(int roomId) {
        return dao.getRoomFullData(roomId);
    }

    // Alternatywna metoda callback (jeśli ktoś woli niż LiveData)
    public void getRoomFullDataOnce(int roomId, Consumer<RoomFullData> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            RoomFullData data = dao.getRoomFullData(roomId).getValue();
            callback.accept(data);
        });
    }
}
