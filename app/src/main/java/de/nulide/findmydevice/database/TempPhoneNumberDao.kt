package de.nulide.findmydevice.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TempPhoneNumberDao {

    @Transaction
    @Query("SELECT * FROM temp_phone_number WHERE number = :number LIMIT 1")
    suspend fun get(number: String): TempPhoneNumberWithSmsPassword?

    @Insert
    suspend fun insert(number: TempPhoneNumber)

    @Query("SELECT * FROM temp_phone_number WHERE added_millis < :cutoffTimeMillis")
    suspend fun getExpired(cutoffTimeMillis: Long): List<TempPhoneNumber>

    @Delete
    suspend fun delete(toDelete: List<TempPhoneNumber>)
}
