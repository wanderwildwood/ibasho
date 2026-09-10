package com.wanderwildwood.ibasho.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPasswordDao {
    @Query("SELECT * FROM notification_password")
    fun getAll(): Flow<List<NotificationPassword>>

    @Query("SELECT * FROM notification_password WHERE password_hash = :passwordHash LIMIT 1")
    suspend fun get(passwordHash: String): NotificationPassword?

    @Insert
    suspend fun insert(password: NotificationPassword)

    @Update
    suspend fun update(password: NotificationPassword)

    @Delete
    suspend fun delete(password: NotificationPassword)
}
