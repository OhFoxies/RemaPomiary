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

    public void insert(RoomInFlat room) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(room));
    }

    public void update(RoomInFlat room) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(room));
    }

    public void delete(RoomInFlat room) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(room));
    }

    public LiveData<List<RoomInFlat>> getRoomsForFlat(int flatId) {
        return dao.getRoomsForFlat(flatId);
    }
    public void insertWithId(RoomInFlat room, Consumer<Long> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = dao.insertWithId(room);
            callback.accept(id);
        });
    }

    public LiveData<RoomFullData> getRoomFullData(int roomId) {
        return dao.getRoomFullData(roomId);
    }

    public void getRoomFullDataOnce(int roomId, Consumer<RoomFullData> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            RoomFullData data = dao.getRoomFullData(roomId).getValue();
            callback.accept(data);
        });
    }
}
