package com.rejner.remapomiary.data.entities;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class BoardsFullData {

    // @Embedded mówi Roomowi, żeby wkleił tu wszystkie pola z encji BoardCommonSpace
    @Embedded
    public BoardCommonSpace board;

    // @Relation mówi, jak połączyć obwody z rozdzielnicą
    @Relation(
            parentColumn = "id",       // Klucz główny w BoardCommonSpace
            entityColumn = "boardId"   // Klucz obcy w CircuitCommonSpace
    )
    public List<CircuitCommonSpace> circuits;
}