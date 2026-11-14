package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowed_apps")
data class AllowedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val sortOrder: Int = 0,
    val isPinnedToDock: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
