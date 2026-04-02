package com.personal.smsapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocalFilterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocalFilter filter);

    @Update
    void update(LocalFilter filter);

    @Delete
    void delete(LocalFilter filter);

    @Query("DELETE FROM local_filters")
    void deleteAll();

    @Query("SELECT * FROM local_filters ORDER BY id ASC")
    LiveData<List<LocalFilter>> getAll();

    @Query("SELECT * FROM local_filters WHERE enabled = 1 ORDER BY id ASC")
    List<LocalFilter> getAllEnabledSync();
}
