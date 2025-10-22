package com.rejner.remapomiary.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.rejner.remapomiary.data.converters.DateConverter;
import com.rejner.remapomiary.data.dao.BlockDao;
import com.rejner.remapomiary.data.dao.CatalogDao;
import com.rejner.remapomiary.data.dao.CircuitDao;
import com.rejner.remapomiary.data.dao.ClientDao;
import com.rejner.remapomiary.data.dao.FlatDao;
import com.rejner.remapomiary.data.dao.OutletMeasurementDao;
import com.rejner.remapomiary.data.dao.ProtocolNumberDao;
import com.rejner.remapomiary.data.dao.RCDDao;
import com.rejner.remapomiary.data.dao.RoomDao;
import com.rejner.remapomiary.data.dao.TemplateDao;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.ProtocolNumber;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Template;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Catalog.class, Block.class, Client.class, Flat.class, Circuit.class, RoomInFlat.class, RCD.class, OutletMeasurement.class, Template.class, ProtocolNumber.class}, version = 14)
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
    public abstract ProtocolNumberDao protocolNumberDao();
    public abstract TemplateDao templateDao();

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `protocolnum` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` INTEGER NOT NULL  DEFAULT 0, creation INTEGER)"
            );
        }
    };
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pomiary_db")
                            .addMigrations(MIGRATION_13_14)
                            .build();

                }
            }
        }
        return INSTANCE;
    }
}
