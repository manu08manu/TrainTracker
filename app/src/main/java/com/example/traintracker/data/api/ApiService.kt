package com.example.traintracker.data.api

import com.example.traintracker.data.model.TimetableResponse
import com.example.traintracker.data.model.TrainResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("v3/uk/train/station/{station}/live.json")
    suspend fun getLiveStationData(
        @Path("station") station: String,
        @Query("type") type: String? = null
    ): TrainResponse

    @GET("v3/uk/train/service/{service}/timetable.json")
    suspend fun getServiceTimetable(
        @Path("service") service: String
    ): TimetableResponse
}
