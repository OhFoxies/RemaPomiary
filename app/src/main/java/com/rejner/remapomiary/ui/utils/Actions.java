package com.rejner.remapomiary.ui.utils;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModelStoreOwner;

import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.CommonSpaceInfo;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;

import java.util.Date;
import java.util.Random;

public class Actions {
    private static final Random rand = new Random();

    public static void saveAndMarkReady(Flat flat, ViewModelStoreOwner owner) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Context context = ((Context) owner).getApplicationContext();
            AppDatabase db = AppDatabase.getDatabase(context);

            db.runInTransaction(() -> {
                String flatNum = flat.number != null ? flat.number.trim() : "";

                // 1. Check if all required CommonSpaceInfo fields are filled for the block.
                if (db.commonSpaceInfoDao().areAllFieldsFilledSync(flat.blockId)) {
                    Log.e("TAG", "Dane uzupelnione");

                    // 2. Ensure "Lokale" room exists for this flat (get it or create it).
                    Flat commonSpace = db.flatDao().getCommonSpaceSync(flat.blockId);
                    if (commonSpace == null) {
                        flat.edition_date = new Date();
                        flat.status = "Pomiar gotowy ✅";
                        db.flatDao().update(flat);
                        return;
                    }
                    RoomInFlat mainRoom = db.roomDao().getMainRoomSync(commonSpace.id);
                    if (mainRoom == null) {
                        Log.e("TAG", "DUtworzono pokoj");

                        mainRoom = new RoomInFlat();
                        mainRoom.flatId = flat.id;
                        mainRoom.name = "Lokale";
                        long id = db.roomDao().insert2(mainRoom);
                        mainRoom.id = (int) id;
                    }

                    if (mainRoom != null) {
                        Log.e("TAG", "Pokoj juz był");

                        // 3. Fetch the CommonSpaceInfo details to populate the measurement.
                        CommonSpaceInfo info = db.commonSpaceInfoDao().getInfoByBlockIdSync(flat.blockId);
                        if (info != null) {
                            Log.e("TAG", "Mam dane common space");

                            String applianceName = "Lokal - " + flatNum;

                            // 4. Check if measurement already exists to avoid duplicates.
                            if (!db.outletMeasurementDao().existsByApplianceSync(mainRoom.id, applianceName)) {
                                Log.e("TAG", "Tworzee mieszkanie ");
                                Log.e("TAG", "Id pokoju " + mainRoom.id);

                                OutletMeasurement om = new OutletMeasurement(mainRoom.id, applianceName);

                                int number;
                                try {
                                    number = Integer.parseInt(flatNum);
                                } catch (NumberFormatException e) {
                                    number = 10;
                                }

                                // Calculate ohms based on flat number and base ohms
                                if (info.ohmsBase != null) {
                                    om.ohms = Math.round(
                                            (((number - 1) / 20.0) * 0.05 + info.ohmsBase + randomOhms())
                                                    * 100.0
                                    ) / 100.0;
                                }

                                om.switchName = info.switchName;
                                om.breakerType = info.breakerType;
                                om.amps = info.amps;
                                om.number = number;
                                om.note = "brak uwag";

                                // 5. Insert the new measurement into the database.
                                Log.d("TAG", om.roomId + " " +  om.id);
                                db.outletMeasurementDao().insert(om);
                            }
                        }
                    }
                }

                // 6. Update flat status and date
                flat.edition_date = new Date();
                flat.status = "Pomiar gotowy ✅";
                db.flatDao().update(flat);
            });
        });
    }

    public static void markUnready(Flat flat, ViewModelStoreOwner owner) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Context context = ((Context) owner).getApplicationContext();
            AppDatabase db = AppDatabase.getDatabase(context);

            db.runInTransaction(() -> {
                String flatNum = flat.number != null ? flat.number.trim() : "";
                Flat commonSpace = db.flatDao().getCommonSpaceSync(flat.blockId);
                if (commonSpace == null) {
                    flat.edition_date = new Date();
                    flat.status = "Pomiar niewykonany ❌";
                    db.flatDao().update(flat);
                    return;
                }
                RoomInFlat mainRoom = db.roomDao().getMainRoomSync(commonSpace.id);
                if (mainRoom != null) {
                    String applianceName = "Lokal - " + flatNum;
                    OutletMeasurement om = db.outletMeasurementDao().getOutletMeasurementSync(mainRoom.id, applianceName);
                    if (om != null) {
                        db.outletMeasurementDao().delete(om);
                    }
                }

                // Update flat status and date
                flat.edition_date = new Date();
                flat.status = "Pomiar niewykonany ❌";
                db.flatDao().update(flat);
            });
        });
    }

    public static double randomOhms() {
        int los = rand.nextInt(11) - 5; // Generates -0.05 to +0.05 range
        return los / 100.0;
    }
}