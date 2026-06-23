package com.rejner.remapomiary.ui.utils;

import android.content.Context;
import androidx.lifecycle.ViewModelStoreOwner;

import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.data.entities.CommonSpaceInfo;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;

import java.util.Date;
import java.util.Random;
import java.util.Set;

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
                            mainRoom.name = Settings.mainRoomName;
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
                                    om.note = Settings.noNotes;
                                    om.amps = info.amps;
                                    om.number = number;

                                    // 5. Insert the new measurement into the database.
                                    db.outletMeasurementDao().insert(om);
                                } else {
                                    OutletMeasurement om = db.outletMeasurementDao().getOutletMeasurementSync(mainRoom.id, applianceName);
                                    if (om.note.equals(Settings.flatNoAccess)) {
                                        om.ohms = 0.0;
                                        om.note = Settings.noNotes;
                                        db.outletMeasurementDao().update(om);
                                    }
                                }
                            }
                        }
// Sprawdzić czy się nie zmieniło f1/f3
                        
                        // NEW LOGIC for BoardCommonSpace and CircuitCommonSpace
                        String boardName = Settings.mainBoardName;
                        BoardCommonSpace board = db.boardCommonSpaceDao().getBoardByNameSync(commonSpace.id, boardName);
                        if (board == null) {
                            board = new BoardCommonSpace();
                            board.flatId = commonSpace.id;
                            board.name = boardName;
                            board.creation_date = new Date();
                            board.type = Settings.installationTypeTNS;
                            long boardId = db.boardCommonSpaceDao().insert(board);
                            board.id = (int) boardId;
                        }

                        String circuitName = "Lokal - " + flatNum;
                        CircuitCommonSpace ccsGet =  db.circuitCommonSpaceDao().getCircuitByNameSync(board.id, circuitName);

                        if (ccsGet == null) {
                            CircuitCommonSpace ccs = new CircuitCommonSpace();
                            ccs.boardId = board.id;
                            ccs.name = circuitName;
                            ccs.notes = Settings.flatGotAccess;
                            // Check if flat has any 3-phase circuits
                            boolean is3f = db.circuitDao().isFlat3fSync(flat.id);
                            ccs.type = is3f ? Settings.installation3f : Settings.installation1f;
                            db.circuitCommonSpaceDao().insert(ccs);
                        } else {
                            boolean is3f = db.circuitDao().isFlat3fSync(flat.id);
                            ccsGet.type = is3f ? Settings.installation3f : Settings.installation1f;
                            if (ccsGet.notes.equals(Settings.flatNoAccess)) {
                                ccsGet.notes = Settings.flatGotAccess;
                            }
                            db.circuitCommonSpaceDao().update(ccsGet);
                        }
                    }
                }

                // 6. Update flat status and metadata
                flat.edition_date = new Date();
                flat.status = Settings.measurementDone;
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
                            om.note = Settings.flatNoAccess;
                            om.ohms = 0.0;

                            db.outletMeasurementDao().update(om);
                        }
                    }

                    // Remove from Common Space board
                    String boardName = Settings.mainBoardName;
                    BoardCommonSpace board = db.boardCommonSpaceDao().getBoardByNameSync(commonSpace.id, boardName);
                    if (board != null) {
                        String circuitName = "Lokal - " + flatNum;
                        CircuitCommonSpace ccs = db.circuitCommonSpaceDao().getCircuitByNameSync(board.id, circuitName);
                        if (ccs != null) {
                            ccs.notes = Settings.flatNoAccess;
                            db.circuitCommonSpaceDao().update(ccs);
                        }
                    }
                }

                // Update flat status and metadata
                flat.edition_date = new Date();
                flat.status = Settings.measurementNotReady;
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