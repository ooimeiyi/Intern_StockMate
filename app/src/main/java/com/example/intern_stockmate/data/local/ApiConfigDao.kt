package com.example.intern_stockmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiConfigDao {
    @Query("SELECT * FROM api_config WHERE id = 1")
    suspend fun getConfig(): ApiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ApiConfigEntity)
}
