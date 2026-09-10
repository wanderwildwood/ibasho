package com.wanderwildwood.ibasho.database

import android.text.format.DateUtils
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

const val TEMP_USAGE_VALIDITY_MILLIS = 10 * 60 * 1000 // 10 min

@Entity(
    tableName = "temp_phone_number",
    indices = [
        // Every phone number must appear at most once. Same as normal phone numbers.
        Index(value = ["number"], unique = true),
        // A password can be used by multiple different phone numbers.
        // The index is for query performance during foreign key lookups.
        Index(value = ["sms_password_id"], unique = false),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SmsPassword::class,
            parentColumns = arrayOf("rowId"),
            childColumns = arrayOf("sms_password_id"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ]
)
data class TempPhoneNumber(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,

    @ColumnInfo("number") val number: String,

    /**
     * SIM-related ID, needed for sending an SMS when this entry expires.
     */
    @ColumnInfo("subscription_id") val subscriptionId: Int,

    /**
     * Timestamp when this entry was added.
     */
    @ColumnInfo("added_millis") val addedMillis: Long,

    @ColumnInfo("sms_password_id") val smsPasswordId: Int,
) {
    fun expiryMillis(): Long {
        return addedMillis + TEMP_USAGE_VALIDITY_MILLIS
    }

    fun expiryPretty(): String {
        return DateUtils.getRelativeTimeSpanString(
            expiryMillis(),
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        ).toString()
    }

    fun isExpired(): Boolean {
        return expiryMillis() < System.currentTimeMillis()
    }
}

data class TempPhoneNumberWithSmsPassword(
    @Embedded val tempPhoneNumber: TempPhoneNumber,

    @Relation(
        parentColumn = "sms_password_id",
        entityColumn = "rowId",
    )
    val smsPassword: SmsPassword,
)
