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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TrainRepository(
    private val stationDao: StationDao,
    private val context: Context
) {
    private val apiService = RetrofitInstance.getApi(context)
    private val gson = Gson()

    suspend fun getLiveStationData(stationCode: String): TrainResponse = 
        apiService.getLiveStationData(stationCode)

    suspend fun getServiceTimetable(serviceId: String): TimetableResponse = 
        apiService.getServiceTimetable(serviceId)

    suspend fun loadStations(): List<Station> = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open("uk_stations.json").use { input ->
                val listType = object : TypeToken<List<Station>>() {}.type
                gson.fromJson<List<Station>>(input.bufferedReader(), listType)
            }
        }.getOrDefault(emptyList())
    }

    fun getFavorites(): Flow<List<FavoriteStation>> = stationDao.getFavorites()

    fun isFavorite(crsCode: String): Flow<Boolean> = stationDao.isFavorite(crsCode)

    suspend fun addFavorite(station: FavoriteStation) = withContext(Dispatchers.IO) {
        stationDao.addFavorite(station)
    }

    suspend fun removeFavorite(station: FavoriteStation) = withContext(Dispatchers.IO) {
        stationDao.removeFavorite(station)
    }
}
