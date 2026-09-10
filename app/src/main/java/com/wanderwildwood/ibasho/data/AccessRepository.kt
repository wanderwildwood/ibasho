package com.wanderwildwood.ibasho.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.FmdPermission
import com.wanderwildwood.ibasho.database.ACCESS_DB_FILENAME
import com.wanderwildwood.ibasho.database.AccessDatabase
import com.wanderwildwood.ibasho.database.NotificationPassword
import com.wanderwildwood.ibasho.database.PhoneNumber
import com.wanderwildwood.ibasho.database.SmsPassword
import com.wanderwildwood.ibasho.database.SmsPasswordWithTempPhoneNumbers
import com.wanderwildwood.ibasho.database.TEMP_USAGE_VALIDITY_MILLIS
import com.wanderwildwood.ibasho.database.TempPhoneNumber
import com.wanderwildwood.ibasho.database.TempPhoneNumberWithSmsPassword
import com.wanderwildwood.ibasho.transports.SmsTransport
import com.wanderwildwood.ibasho.utils.SingletonHolder
import com.wanderwildwood.ibasho.utils.log
import com.wanderwildwood.ibasho.utils.normalizePhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AccessRepository private constructor(private val context: Context) {

    companion object : SingletonHolder<AccessRepository, Context>(::AccessRepository) {
        val TAG = AccessRepository::class.simpleName
    }

    private var db = openDbInt()

    private fun openDbInt(): AccessDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AccessDatabase::class.java,
            ACCESS_DB_FILENAME,
        ).build()
    }

    fun openDb() {
        db = openDbInt()
    }

    fun closeDb() {
        db.close()
    }

    /**
     * Force the DB to write the WAL to the main DB file.
     * This is useful before exports, because it means that we don't need to include the WAL in the export.
     * See https://sqlite.org/wal.html.
     */
    fun forceWriteToDisk() {
        // Force WAL write to main DB file
        db.query(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))

        // Force main DB file write from memory to disk
        closeDb()
        openDb()
    }

    /*
     * WARNING: All phone numbers MUST be normalized before being inserted or being looked up!
     */

    /* ------- Phone numbers ------- */

    fun getPhoneNumbers(): Flow<List<PhoneNumber>> {
        return db.phoneNumberDao().getAll()
    }

    suspend fun getPhoneNumber(phoneNumber: String): PhoneNumber? {
        val number = normalizePhoneNumber(context, phoneNumber) ?: return null
        val entity = db.phoneNumberDao().get(number)
        return entity
    }

    // XXX: Dispatchers.IO appears to be needed. Otherwise, the getPhoneNumbers Flow won't emit the new value.
    suspend fun insertPhoneNumber(phoneNumber: PhoneNumber) = withContext(Dispatchers.IO) {
        val number = normalizePhoneNumber(context, phoneNumber.number) ?: return@withContext
        val norm = phoneNumber.copy(number = number)
        db.phoneNumberDao().insert(norm)
    }

    suspend fun updatePhoneNumber(phoneNumber: PhoneNumber) = withContext(Dispatchers.IO) {
        db.phoneNumberDao().update(phoneNumber)
    }

    suspend fun deletePhoneNumber(phoneNumber: PhoneNumber) = withContext(Dispatchers.IO) {
        db.phoneNumberDao().delete(phoneNumber)
    }

    suspend fun migratePhoneAllowListToDb(oldList: AllowlistModel) {
        for (old in oldList) {
            // Due to https://gitlab.com/fmd-foss/fmd-android/-/work_items/426, the migration may
            // have failed in 0.16.0, then the user added the number again, and then in 0.16.1
            // this migration runs again. Skip these duplicate numbers.
            if (getPhoneNumber(old.number) != null) {
                continue
            }
            val number = normalizePhoneNumber(context, old.number) ?: continue
            val new = PhoneNumber(0, old.name, number, FmdPermission.ALL)
            db.phoneNumberDao().insert(new)
        }
    }

    /* ------- SMS Passwords ------- */

    fun getSmsPasswords(): Flow<List<SmsPasswordWithTempPhoneNumbers>> {
        return db.smsPasswordDao().getAll()
    }

    suspend fun getSmsPassword(passwordHash: String): SmsPassword? {
        return db.smsPasswordDao().get(passwordHash)
    }

    suspend fun insertSmsPassword(password: SmsPassword) = withContext(Dispatchers.IO) {
        db.smsPasswordDao().insert(password)
    }

    suspend fun updateSmsPassword(password: SmsPassword) = withContext(Dispatchers.IO) {
        db.smsPasswordDao().update(password)
    }

    suspend fun deleteSmsPassword(password: SmsPassword) = withContext(Dispatchers.IO) {
        db.smsPasswordDao().delete(password)
    }

    /* ------- Temporary Phone Numbers ------- */

    suspend fun getTempPhoneNumber(number: String): TempPhoneNumberWithSmsPassword? {
        val number = normalizePhoneNumber(context, number) ?: return null
        return db.tempPhoneNumberDao().get(number)
    }

    suspend fun insertTempPhoneNumber(
        number: String,
        subscriptionId: Int,
        smsPassword: SmsPassword,
    ) = withContext(Dispatchers.IO) {
        val normNumber = normalizePhoneNumber(context, number) ?: return@withContext
        val addedMillis = System.currentTimeMillis()
        val tempNumber =
            TempPhoneNumber(0, normNumber, subscriptionId, addedMillis, smsPassword.rowId)

        db.tempPhoneNumberDao().insert(tempNumber)
    }

    private suspend fun deleteExpiredTempPhoneNumbers(): List<TempPhoneNumber> =
        withContext(Dispatchers.IO) {
            val cutoffTimeMillis = System.currentTimeMillis() - TEMP_USAGE_VALIDITY_MILLIS
            val toDelete = db.tempPhoneNumberDao().getExpired(cutoffTimeMillis)
            db.tempPhoneNumberDao().delete(toDelete)
            return@withContext toDelete
        }

    suspend fun removeAndNotifyExpiredTempPhoneNumbers() {
        val expired = deleteExpiredTempPhoneNumbers()

        for (item in expired) {
            val transport = SmsTransport(context, item.number, item.subscriptionId)
            transport.send(context, context.getString(R.string.temporary_allowlist_expired))
            context.log().i(TAG, "Phone number expired ${item.number}")
        }
    }

    /* ------- Notification Passwords ------- */

    fun getNotificationPasswords(): Flow<List<NotificationPassword>> {
        return db.notificationPasswordDao().getAll()
    }

    suspend fun getNotificationPassword(passwordHash: String): NotificationPassword? {
        return db.notificationPasswordDao().get(passwordHash)
    }

    suspend fun insertNotificationPassword(password: NotificationPassword) =
        withContext(Dispatchers.IO) {
            db.notificationPasswordDao().insert(password)
        }

    suspend fun updateNotificationPassword(password: NotificationPassword) =
        withContext(Dispatchers.IO) {
            db.notificationPasswordDao().update(password)
        }

    suspend fun deleteNotificationPassword(password: NotificationPassword) =
        withContext(Dispatchers.IO) {
            db.notificationPasswordDao().delete(password)
        }

}
