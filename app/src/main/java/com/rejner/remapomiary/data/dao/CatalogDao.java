package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.rejner.remapomiary.data.entities.Catalog;

import java.util.Date;
import java.util.List;

@Dao
public interface CatalogDao {
    @Insert
    long insert(Catalog catalog);

    @Query("SELECT * FROM catalogs ORDER BY creation_date DESC")
    List<Catalog> getAllCatalogsByCreation();


    @Query("SELECT * FROM catalogs ORDER BY edition_time DESC ")
    List<Catalog> getAllCatalogsByEdition();

    @Query("SELECT * FROM catalogs WHERE title = :title")
    Catalog getCatalogByName(String title);

    @Query("UPDATE catalogs SET city= :city, street = :street, postal_code = :postal_code, title = :title, edition_time = :time WHERE id = :id")
    void update(String city, String street, String postal_code, String title, Date time, int id);

    @Delete
    void delete(Catalog catalog);

    @Query("SELECT * FROM catalogs WHERE id=:id")
    Catalog getCatalogById(int id);

    @Query("UPDATE catalogs SET edition_time = :time WHERE id = :catalogId")
    void updateEdition(int catalogId, Date time);
}
