package com.example.traintracker.data.repository

import android.content.Context
import com.example.traintracker.data.api.RetrofitInstance
import com.example.traintracker.data.db.StationDao
import com.example.traintracker.data.model.FavoriteStation
import com.example.traintracker.data.model.Station
import com.example.traintracker.data.model.TimetableResponse
import com.example.traintracker.data.model.TrainResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrainRepository(
    private val stationDao: StationDao,
    private val context: Context
) {
    private val apiService = RetrofitInstance.getApi(context)

    suspend fun getLiveStationData(stationCode: String): TrainResponse = withContext(Dispatchers.IO) {
        apiService.getLiveStationData(stationCode)
    }

    suspend fun getServiceTimetable(serviceId: String): TimetableResponse = withContext(Dispatchers.IO) {
        apiService.getServiceTimetable(serviceId)
    }

    suspend fun loadStations(): List<Station> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open("uk_stations.json")
                .bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Station>>() {}.type
            Gson().fromJson(json, listType)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getFavorites() = stationDao.getFavorites()

    suspend fun addFavorite(station: FavoriteStation) = withContext(Dispatchers.IO) {
        stationDao.addFavorite(station)
    }

    suspend fun removeFavorite(station: FavoriteStation) = withContext(Dispatchers.IO) {
        stationDao.removeFavorite(station)
    }
}
