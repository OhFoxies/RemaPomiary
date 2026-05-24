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
import com.rejner.remapomiary.data.dao.BoardCommonSpaceDao;
import com.rejner.remapomiary.data.dao.CatalogDao;
import com.rejner.remapomiary.data.dao.CircuitCommonSpaceDao;
import com.rejner.remapomiary.data.dao.CircuitDao;
import com.rejner.remapomiary.data.dao.ClientDao;
import com.rejner.remapomiary.data.dao.ContractorsDao;
import com.rejner.remapomiary.data.dao.FlatDao;
import com.rejner.remapomiary.data.dao.OutletMeasurementDao;
import com.rejner.remapomiary.data.dao.ProtocolNumberDao;
import com.rejner.remapomiary.data.dao.RCDDao;
import com.rejner.remapomiary.data.dao.RoomDao;
import com.rejner.remapomiary.data.dao.SignatureDao;
import com.rejner.remapomiary.data.dao.TemplateDao;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.entities.Contractors;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.ProtocolNumber;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Signature;
import com.rejner.remapomiary.data.entities.Template;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Contractors.class, Signature.class, Catalog.class, Block.class, CircuitCommonSpace.class, BoardCommonSpace.class, Client.class, Flat.class, Circuit.class, RoomInFlat.class, RCD.class, OutletMeasurement.class, Template.class, ProtocolNumber.class}, version = 20)
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
    public abstract BoardCommonSpaceDao boardCommonSpaceDao();
    public abstract CircuitCommonSpaceDao circuitCommonSpaceDao();
    public abstract ContractorsDao contractorsDao();

    public abstract SignatureDao signatureDao();
    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE protocolnum ADD column is_current INT NOT NULL default 1;"
            );
        }
    };

    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE flat ADD column isCommonSpace INT NOT NULL default 0;"
            );
        }
    };

    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the board_common_space table
            // Note: Assuming your Flat table is named "Flat" or "flat" in the database.
            // Adjust the REFERENCES `Flat`(`id`) below if your table name is different.
            // Assuming 'creation_date' uses a TypeConverter that saves Dates as INTEGER (timestamps).
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `board_common_space` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`flatId` INTEGER NOT NULL, " +
                            "`name` TEXT, " +
                            "`creation_date` INTEGER, " +
                            "FOREIGN KEY(`flatId`) REFERENCES `flat`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            // Create the circuit_common_space table
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `circuit_common_space` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`boardId` INTEGER NOT NULL, " +
                            "`name` TEXT, " +
                            "`type` TEXT, " +
                            "FOREIGN KEY(`boardId`) REFERENCES `board_common_space`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );


            database.execSQL("" +
                    "CREATE TABLE IF NOT EXISTS `contractors` (\n" +
                    "    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, \n" +
                    "    `name` TEXT, \n" +
                    "    `surname` TEXT, \n" +
                    "    `e_permit` TEXT, \n" +
                    "    `d_permit` TEXT, \n" +
                    "    `type` INTEGER NOT NULL\n" +
                    ");");
        }
    };

    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE board_common_space ADD column type TEXT default 'TN-S';"
            );
            database.execSQL(
                    "ALTER TABLE board_common_space ADD column notes TEXT default '';"
            );
        }
    };

    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `signatures` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`flatId` INTEGER NOT NULL, " +
                    "`signatureData` BLOB, " +
                    "FOREIGN KEY(`flatId`) REFERENCES `flat`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");

            // 2. Tworzenie indeksu dokładnie tak, jak oczekuje tego Room
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_signatures_flatId` ON `signatures` (`flatId`)");
        }
    };

    static final Migration MIGRATION_19_20= new Migration(19, 20) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `signatures` ADD COLUMN `signerName` TEXT");
            // Dodanie kolumny na datę (jako INTEGER/timestamp)
            database.execSQL("ALTER TABLE `signatures` ADD COLUMN `signatureDate` INTEGER");
        }
    };


    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pomiary_db")
                            .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                            .build();

                }
            }
        }
        return INSTANCE;
    }
}
