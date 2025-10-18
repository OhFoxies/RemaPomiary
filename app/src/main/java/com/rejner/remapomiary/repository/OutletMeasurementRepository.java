package com.rejner.remapomiary.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.rejner.remapomiary.data.dao.OutletMeasurementDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.OutletMeasurement;

import java.util.List;
import java.util.function.Consumer;

public class OutletMeasurementRepository {
    private final OutletMeasurementDao dao;

    public OutletMeasurementRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.outletMeasurementDao();
    }

    public void insert(OutletMeasurement outletMeasurement, Consumer<Long> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long lastId = dao.insert(outletMeasurement);
            callback.accept(lastId);
        });
    }

    public void update(OutletMeasurement outletMeasurement, Runnable onFinished) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.update(outletMeasurement);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    public void delete(OutletMeasurement outletMeasurement, Runnable onFinished) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.delete(outletMeasurement);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    public LiveData<List<OutletMeasurement>> getMeasurementsForRoom(int roomId) {
        return dao.getMeasurementsForRoom(roomId);
    }
}
