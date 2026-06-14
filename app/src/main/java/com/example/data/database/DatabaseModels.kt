package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCanvaDesign: Boolean,
    val imagePath: String?, // Stores local Uri or base64
    val editorStateJson: String, // Serialized stack (adjustments, shapes, stickers)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_presets")
data class CustomPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val exposure: Float,
    val temperature: Float,
    val tint: Float,
    val vibrance: Float,
    val vignette: Float,
    val sharpness: Float,
    val filterType: String
)

@Entity(tableName = "analytics")
data class AnalyticsEntity(
    @PrimaryKey val id: Int = 1,
    val totalProjects: Int = 0,
    val totalExports: Int = 0,
    val mostUsedFilter: String = "None",
    val totalEditingTimeSeconds: Long = 0,
    val storageUsageBytes: Long = 0
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface CustomPresetDao {
    @Query("SELECT * FROM custom_presets ORDER BY id DESC")
    fun getAllPresets(): Flow<List<CustomPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CustomPresetEntity): Long

    @Query("DELETE FROM custom_presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics WHERE id = 1 LIMIT 1")
    suspend fun getAnalytics(): AnalyticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: AnalyticsEntity)
}
