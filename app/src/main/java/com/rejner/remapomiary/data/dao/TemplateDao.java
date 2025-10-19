package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.Template;

import java.util.List;
@Dao
public interface TemplateDao {
    @Insert
    void insert(Template template);

    @Update
    void update(Template template);

    @Delete
    void delete(Template template);

    @Query("SELECT * FROM templates WHERE flatId=:flatId")
    Template getTemplateByFlatId(int flatId);

    @Query("SELECT t.*\n" +
            "FROM templates t\n" +
            "JOIN flat f ON t.flatId = f.id\n" +
            "JOIN blocks b ON f.blockId = b.id\n" +
            "JOIN catalogs c ON b.catalogId = c.id\n" +
            "WHERE c.id = :catalogId;")
    LiveData<List<Template>> getTemplatesInCatalog(int catalogId);

    @Query("SELECT EXISTS (\n" +
            "    SELECT 1\n" +
            "    FROM templates t\n" +
            "    JOIN flat f ON t.flatId = f.id\n" +
            "    JOIN blocks b ON f.blockId = b.id\n" +
            "    WHERE b.catalogId = :catalogId\n" +
            "      AND t.name = :templateName\n" +
            ") AS exists_in_catalog;")
    Boolean doesTemplateNameExists(String templateName, int catalogId);


}
