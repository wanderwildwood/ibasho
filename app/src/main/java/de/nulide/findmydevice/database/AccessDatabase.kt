package de.nulide.findmydevice.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

const val ACCESS_DB_FILENAME = "access.db"

@Database(
    entities = [
        PhoneNumber::class,
        SmsPassword::class,
        TempPhoneNumber::class,
        NotificationPassword::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]
)
abstract class AccessDatabase : RoomDatabase() {
    abstract fun phoneNumberDao(): PhoneNumberDao

    abstract fun smsPasswordDao(): SmsPasswordDao

    abstract fun tempPhoneNumberDao(): TempPhoneNumberDao

    abstract fun notificationPasswordDao(): NotificationPasswordDao
}
