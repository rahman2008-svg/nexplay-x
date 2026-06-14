package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LensoraRepository(private val database: LensoraDatabase) {
    private val projectDao = database.projectDao()
    private val presetDao = database.customPresetDao()
    private val analyticsDao = database.analyticsDao()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allPresets: Flow<List<CustomPresetEntity>> = presetDao.getAllPresets()

    suspend fun getProjectById(id: Int): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity): Long = withContext(Dispatchers.IO) {
        projectDao.insertProject(project)
    }

    suspend fun deleteProject(id: Int) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }

    suspend fun savePreset(preset: CustomPresetEntity): Long = withContext(Dispatchers.IO) {
        presetDao.insertPreset(preset)
    }

    suspend fun deletePreset(id: Int) = withContext(Dispatchers.IO) {
        presetDao.deletePresetById(id)
    }

    suspend fun getAnalytics(): AnalyticsEntity = withContext(Dispatchers.IO) {
        analyticsDao.getAnalytics() ?: AnalyticsEntity()
    }

    suspend fun incrementTotalProjects() = withContext(Dispatchers.IO) {
        val current = getAnalytics()
        analyticsDao.insertAnalytics(current.copy(totalProjects = current.totalProjects + 1))
    }

    suspend fun incrementTotalExports() = withContext(Dispatchers.IO) {
        val current = getAnalytics()
        analyticsDao.insertAnalytics(current.copy(totalExports = current.totalExports + 1))
    }

    suspend fun updateMostUsedFilter(filter: String) = withContext(Dispatchers.IO) {
        val current = getAnalytics()
        analyticsDao.insertAnalytics(current.copy(mostUsedFilter = filter))
    }

    suspend fun addEditingTime(seconds: Long) = withContext(Dispatchers.IO) {
        val current = getAnalytics()
        analyticsDao.insertAnalytics(current.copy(totalEditingTimeSeconds = current.totalEditingTimeSeconds + seconds))
    }

    suspend fun updateStorageUsage(bytes: Long) = withContext(Dispatchers.IO) {
        val current = getAnalytics()
        analyticsDao.insertAnalytics(current.copy(storageUsageBytes = current.storageUsageBytes + bytes))
    }
}
