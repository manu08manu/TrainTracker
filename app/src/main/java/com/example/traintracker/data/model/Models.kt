package com.example.traintracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Station(
    val stationName: String,
    val crsCode: String,
    val lat: Double? = null,
    val long: Double? = null
)

@Entity(tableName = "favorites")
data class FavoriteStation(
    @PrimaryKey val crsCode: String,
    val stationName: String
)

data class TrainResponse(
    val departures: Departures?,
    val arrivals: Arrivals?
)

data class Departures(
    val all: List<TrainService>
)

data class Arrivals(
    val all: List<TrainService>
)

data class TrainService(
    val aimedDepartureTime: String?,
    val expectedDepartureTime: String?,
    val aimedArrivalTime: String?,
    val expectedArrivalTime: String?,
    val destinationName: String?,
    val originName: String?,
    val platform: String?,
    val status: String?,
    val service: String?,
    val operatorName: String?,
    val operator: String?
)

data class TimetableResponse(
    val stops: List<Stop>
)

data class Stop(
    val stationName: String,
    val aimedArrivalTime: String?,
    val expectedArrivalTime: String?,
    val aimedDepartureTime: String?,
    val expectedDepartureTime: String?
)
