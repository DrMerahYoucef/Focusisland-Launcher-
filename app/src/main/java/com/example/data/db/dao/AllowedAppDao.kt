package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.AllowedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedAppDao {
    @Query("SELECT * FROM allowed_apps ORDER BY sortOrder ASC, appName COLLATE NOCASE ASC")
    fun getAllAllowed(): Flow<List<AllowedAppEntity>>

    @Query("SELECT * FROM allowed_apps ORDER BY sortOrder ASC, appName COLLATE NOCASE ASC")
    suspend fun getAllAllowedOnce(): List<AllowedAppEntity>

    @Query("SELECT packageName FROM allowed_apps")
    suspend fun getAllowedPackagesOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: AllowedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AllowedAppEntity>)

    @Query("DELETE FROM allowed_apps WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("DELETE FROM allowed_apps")
    suspend fun deleteAll()
}
