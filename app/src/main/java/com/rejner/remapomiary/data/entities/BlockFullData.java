package com.rejner.remapomiary.data.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

public class BlockFullData {
    @Embedded
    public Block block;

    @Relation(
            parentColumn = "catalogId",
            entityColumn = "id"
    )
    public Catalog catalog;

    @Relation(
            parentColumn = "clientId",
            entityColumn = "id"
    )
    public Client client;

    // Getter z zabezpieczeniem przed null
    public Client getClient() {
        if (client == null) {
            Client defaultClient = new Client("-","-","-","Zleceniodawca został usunięty", -1);

            // ustaw pozostałe pola jeśli potrzebne
            return defaultClient;
        }
        return client;
    }
}
