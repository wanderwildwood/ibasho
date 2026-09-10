package com.wanderwildwood.ibasho.database

interface AccessItem {
    fun getItemPermission(): Long

    fun toDisplayLabel(): String
}
