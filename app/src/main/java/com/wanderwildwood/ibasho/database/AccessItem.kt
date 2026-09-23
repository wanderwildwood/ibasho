package com.wanderwildwood.ibasho.database

import android.content.Context

interface AccessItem {
    fun getItemPermission(): Long

    fun toDisplayLabel(context: Context): String
}
