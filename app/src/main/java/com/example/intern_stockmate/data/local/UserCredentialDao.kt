package com.example.intern_stockmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserCredentialDao {
    @Query("SELECT * FROM user_credentials WHERE userId = :userId LIMIT 1")
    suspend fun getCredential(userId: String): UserCredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredential(credential: UserCredentialEntity)
}