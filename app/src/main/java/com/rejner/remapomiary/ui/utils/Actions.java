package com.rejner.remapomiary.ui.utils;

import android.content.Context;
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
                    
                    Flat commonSpace = db.flatDao().getCommonSpaceSync(flat.blockId);
                    if (commonSpace != null) {
                        // 2. Ensure "Lokale" room exists for this block in the Common Space flat.
                        RoomInFlat mainRoom = db.roomDao().getMainRoomSync(commonSpace.id);
                        if (mainRoom == null) {
                            mainRoom = new RoomInFlat();
                            mainRoom.flatId = commonSpace.id;
                            mainRoom.name = "Lokale";
                            long id = db.roomDao().insert2(mainRoom);
                            mainRoom.id = (int) id;
                        }

                        if (mainRoom != null) {
                            // 3. Fetch the CommonSpaceInfo details to populate the measurement.
                            CommonSpaceInfo info = db.commonSpaceInfoDao().getInfoByBlockIdSync(flat.blockId);
                            if (info != null) {
                                String applianceName = "Lokal - " + flatNum;

                                // 4. Check if measurement already exists to avoid duplicates.
                                if (!db.outletMeasurementDao().existsByApplianceSync(mainRoom.id, applianceName)) {
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
                                    om.note = "Pomiar Wykonany";

                                    // 5. Insert the new measurement into the database.
                                    db.outletMeasurementDao().insert(om);
                                }
                            }
                        }
                    }
                }

                // 6. Update flat status and metadata
                flat.edition_date = new Date();
                flat.status = "Pomiar gotowy ✅";
                db.flatDao().update(flat);
                
                // Update block and catalog edition time
                db.blockDao().updateEditionTime(flat.blockId, flat.edition_date);
                int catalogId = db.blockDao().getBlockById(flat.blockId).block.catalogId;
                db.catalogDao().updateEdition(catalogId, flat.edition_date);
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
                if (commonSpace != null) {
                    RoomInFlat mainRoom = db.roomDao().getMainRoomSync(commonSpace.id);
                    if (mainRoom != null) {
                        String applianceName = "Lokal - " + flatNum;
                        OutletMeasurement om = db.outletMeasurementDao().getOutletMeasurementSync(mainRoom.id, applianceName);
                        if (om != null) {
                            db.outletMeasurementDao().delete(om);
                        }
                    }
                }

                // Update flat status and metadata
                flat.edition_date = new Date();
                flat.status = "Pomiar niewykonany ❌";
                db.flatDao().update(flat);
                
                db.blockDao().updateEditionTime(flat.blockId, flat.edition_date);
                int catalogId = db.blockDao().getBlockById(flat.blockId).block.catalogId;
                db.catalogDao().updateEdition(catalogId, flat.edition_date);
            });
        });
    }

    public static double randomOhms() {
        int los = rand.nextInt(11) - 5; // -0.05 to +0.05 range
        return los / 100.0;
    }
}
