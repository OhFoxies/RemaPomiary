package com.rejner.remapomiary.data.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;


public class FlatFullData {

    @Embedded
    public Flat flat;

    @Relation(
            parentColumn = "id",
            entityColumn = "flatId"
    )
    public List<RoomInFlat> rooms;

    @Relation(
            parentColumn = "id",
            entityColumn = "flatId"
    )
    public List<Circuit> circuits;

    @Relation(
            parentColumn = "id",
            entityColumn = "flatId"
    )
    public List<RCD> rcds;


}