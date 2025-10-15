package com.rejner.remapomiary.data.entities; // Or your entities package

import androidx.room.Embedded;
import androidx.room.Relation;

import com.rejner.remapomiary.data.entities.RoomInFlat; // Ensure this import is correct

import java.util.List;

/**
 * A data class that encapsulates a RoomInFlat with its related OutletMeasurements.
 * This is used to query and hold a complete representation of a room's data
 * from the database in a single, atomic operation.
 */
public class RoomFullData {

    @Embedded
    public RoomInFlat room;

    @Relation(
            parentColumn = "id",
            entityColumn = "roomId"
    )
    public List<OutletMeasurement> measurements;
}
