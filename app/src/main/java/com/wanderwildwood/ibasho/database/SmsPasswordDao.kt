package com.wanderwildwood.ibasho.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsPasswordDao {
    @Transaction
    @Query("SELECT * FROM sms_password")
    fun getAll(): Flow<List<SmsPasswordWithTempPhoneNumbers>>

    @Query("SELECT * FROM sms_password WHERE password_hash = :passwordHash LIMIT 1")
    suspend fun get(passwordHash: String): SmsPassword?

    @Insert
    suspend fun insert(password: SmsPassword)

    @Update
    suspend fun update(password: SmsPassword)

    @Delete
    suspend fun delete(password: SmsPassword)
}
