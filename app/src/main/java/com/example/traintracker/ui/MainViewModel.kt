package com.example.traintracker.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.traintracker.data.Constants
import com.example.traintracker.data.db.StationDatabase
import com.example.traintracker.data.model.FavoriteStation
import com.example.traintracker.data.model.Station
import com.example.traintracker.data.model.Stop
import com.example.traintracker.data.model.TrainService
import com.example.traintracker.data.repository.TrainRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val services: List<TrainService>) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class CallingPatternUiState {
    object Idle : CallingPatternUiState()
    object Loading : CallingPatternUiState()
    data class Success(val stops: List<Stop>) : CallingPatternUiState()
    data class Error(val message: String) : CallingPatternUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrainRepository = TrainRepository(
        StationDatabase.getDatabase(application).stationDao(),
        application
    )

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _isArrivals = MutableStateFlow(false)
    val isArrivals: StateFlow<Boolean> = _isArrivals.asStateFlow()

    private val _currentCrsCode = MutableStateFlow<String?>(null)

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    val uiState: StateFlow<UiState> = combine(refreshTrigger, _currentCrsCode, _isArrivals) { _, code, arrivals ->
        code to arrivals
    }.flatMapLatest { (code, arrivals) ->
        if (code == null) flowOf(UiState.Idle)
        else flow<UiState> {
            emit(UiState.Loading)
            try {
                val response = repository.getLiveStationData(code)
                val list = if (arrivals) response.arrivals?.all else response.departures?.all
                if (list.isNullOrEmpty()) emit(UiState.Error("No services found"))
                else emit(UiState.Success(list.take(20)))
            } catch (_: Exception) {
                emit(UiState.Error("Failed to load data"))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Idle)

    val lastUpdated: StateFlow<String?> = uiState.map {
        if (it is UiState.Success) LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isCurrentStationFavorite: StateFlow<Boolean> = _currentCrsCode
        .flatMapLatest { code ->
            if (code == null) flowOf(false)
            else repository.getFavorites().map { favs -> favs.any { it.crsCode == code } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val favorites: StateFlow<List<FavoriteStation>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _callingPatternUiState = MutableStateFlow<CallingPatternUiState>(CallingPatternUiState.Idle)
    val callingPatternUiState: StateFlow<CallingPatternUiState> = _callingPatternUiState.asStateFlow()

    init {
        loadStations()
        refreshTrigger.tryEmit(Unit)
        
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(Constants.REFRESH_INTERVAL_MS)
                refreshTrigger.emit(Unit)
            }
        }
    }

    private fun loadStations() {
        viewModelScope.launch {
            _stations.value = repository.loadStations()
        }
    }

    fun setMode(arrivals: Boolean) {
        _isArrivals.value = arrivals
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    fun fetchStationData(code: String) {
        _currentCrsCode.value = code
    }

    fun updateCurrentStation(code: String) {
        fetchStationData(code)
    }

    fun fetchServiceTimetable(serviceId: String) {
        viewModelScope.launch {
            _callingPatternUiState.value = CallingPatternUiState.Loading
            try {
                val response = repository.getServiceTimetable(serviceId)
                _callingPatternUiState.value = CallingPatternUiState.Success(response.stops)
            } catch (_: Exception) {
                _callingPatternUiState.value = CallingPatternUiState.Error("Failed to load timetable")
            }
        }
    }

    fun clearServiceTimetable() {
        _callingPatternUiState.value = CallingPatternUiState.Idle
    }

    fun removeFavorite(station: FavoriteStation) {
        viewModelScope.launch {
            repository.removeFavorite(station)
        }
    }

    fun toggleFavorite(): Boolean {
        val code = _currentCrsCode.value ?: return false
        val station = _stations.value.find { it.crsCode == code } ?: return false
        viewModelScope.launch {
            val isFav = favorites.value.any { it.crsCode == code }
            if (isFav) repository.removeFavorite(FavoriteStation(code, station.stationName))
            else repository.addFavorite(FavoriteStation(code, station.stationName))
        }
        return true
    }

    fun findNearestStation(location: Location): Station? {
        return _stations.value
            .filter { it.lat != null && it.long != null }
            .minByOrNull { station ->
                val stationLoc = Location("").apply {
                    latitude = station.lat!!
                    longitude = station.long!!
                }
                location.distanceTo(stationLoc)
            }
    }
}
