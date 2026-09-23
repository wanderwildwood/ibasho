package com.wanderwildwood.ibasho.database

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderwildwood.ibasho.commands.FmdPermission
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "notification_password",
    indices = [
        Index(value = ["password_hash"], unique = true),
    ],
)
data class NotificationPassword(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,

    @ColumnInfo("label") val label: String,

    @ColumnInfo("password_hash") val passwordHash: String,

    @ColumnInfo("permissions") val permission: Long = FmdPermission.DEFAULT,
) : AccessItem, Parcelable {

    override fun getItemPermission(): Long {
        return permission
    }

    override fun toDisplayLabel(context: Context): String {
        return label
    }
}
