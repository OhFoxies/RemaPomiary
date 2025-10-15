package com.rejner.remapomiary.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.rejner.remapomiary.data.converters.DateConverter;
import com.rejner.remapomiary.data.dao.BlockDao;
import com.rejner.remapomiary.data.dao.CatalogDao;
import com.rejner.remapomiary.data.dao.CircuitDao;
import com.rejner.remapomiary.data.dao.ClientDao;
import com.rejner.remapomiary.data.dao.FlatDao;
import com.rejner.remapomiary.data.dao.OutletMeasurementDao;
import com.rejner.remapomiary.data.dao.RCDDao;
import com.rejner.remapomiary.data.dao.RoomDao;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Catalog.class, Block.class, Client.class, Flat.class, Circuit.class, RoomInFlat.class, RCD.class, OutletMeasurement.class}, version = 4)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    public abstract CatalogDao catalogDao();
    public abstract ClientDao clientDao();
    public abstract BlockDao blockDao();
    public abstract FlatDao flatDao();
    public abstract RCDDao rcdDao();
    public abstract CircuitDao circuitDao();
    public abstract OutletMeasurementDao outletMeasurementDao();
    public abstract RoomDao roomDao();

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pomiary_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
