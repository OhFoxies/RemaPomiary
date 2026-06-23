package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.RoomDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.RoomFullData;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.ui.utils.Settings;

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

    public void getMainRoomForCommon(int flatId, Consumer<RoomInFlat> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            RoomInFlat mainRoom = dao.getMainCommonSpaceRoom(flatId);
            callback.accept(mainRoom);
        });
    }
    public interface OnRoomReadyCallback {
        void onReady(RoomInFlat room);
    }

    public void getOrCreateMainRoom(int flatId, OnRoomReadyCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Sprawdzamy synchronicznie w tle czy istnieje
            RoomInFlat room = dao.getMainRoomSync(flatId);

            // 2. Jeśli nie istnieje, tworzymy go od razu w tym samym wątku
            if (room == null) {
                room = new RoomInFlat();
                room.flatId = flatId;
                room.name = Settings.mainRoomName; // Twoja domyślna nazwa dla Main Room

                long generatedId = dao.insert2(room);
                room.id = (int) generatedId; // Przypisujemy wygenerowane ID do obiektu
            }

            // 3. Przerzucamy gotowy obiekt z powrotem do wątku głównego (UI)
            RoomInFlat finalRoom = room;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.onReady(finalRoom);
            });
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
