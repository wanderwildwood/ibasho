package com.wanderwildwood.ibasho.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.wanderwildwood.ibasho.commands.FmdPermission
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "sms_password",
    indices = [
        Index(value = ["password_hash"], unique = true),
    ],
)
data class SmsPassword(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,

    @ColumnInfo("label") val label: String,

    @ColumnInfo("password_hash") val passwordHash: String,

    @ColumnInfo("permissions") val permission: Long = FmdPermission.DEFAULT,
) : AccessItem, Parcelable {

    override fun getItemPermission(): Long {
        return permission
    }

    override fun toDisplayLabel(): String {
        return label
    }
}

data class SmsPasswordWithTempPhoneNumbers(
    @Embedded val smsPassword: SmsPassword,

    @Relation(
        parentColumn = "rowId",
        entityColumn = "sms_password_id",
    )
    val tempPhoneNumbers: List<TempPhoneNumber>,
) : AccessItem {

    override fun getItemPermission(): Long {
        return smsPassword.getItemPermission()
    }

    override fun toDisplayLabel(): String {
        return smsPassword.toDisplayLabel()
    }
}
