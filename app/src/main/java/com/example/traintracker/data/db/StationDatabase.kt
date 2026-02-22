package com.example.traintracker.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.traintracker.data.model.FavoriteStation
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM favorites")
    fun getFavorites(): Flow<List<FavoriteStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(station: FavoriteStation)

    @Delete
    suspend fun removeFavorite(station: FavoriteStation)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE crsCode = :crsCode)")
    fun isFavorite(crsCode: String): Flow<Boolean>
}

@Database(entities = [FavoriteStation::class], version = 1)
abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var INSTANCE: StationDatabase? = null

        fun getDatabase(context: Context): StationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StationDatabase::class.java,
                    "station_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
