package com.example.intern_stockmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserCredentialEntity::class], version = 1, exportSchema = false)
abstract class UserCredentialDatabase : RoomDatabase() {
    abstract fun userCredentialDao(): UserCredentialDao

    companion object {
        @Volatile
        private var INSTANCE: UserCredentialDatabase? = null

        fun getInstance(context: Context): UserCredentialDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserCredentialDatabase::class.java,
                    "user_credential_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}