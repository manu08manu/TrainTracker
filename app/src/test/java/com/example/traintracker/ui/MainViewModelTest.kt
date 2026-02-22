package com.example.traintracker.ui

import android.app.Application
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.traintracker.data.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MainViewModel(context)
    }

    @Test
    fun `findNearestStation returns correct station`() {
        val stationA = Station("Station A", "AAA", 51.5, -0.1)
        val stationB = Station("Station B", "BBB", 52.5, -1.1)
        
        // Access private _stations flow using reflection for testing
        val field = viewModel.javaClass.getDeclaredField("_stations")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stationsFlow = field.get(viewModel) as MutableStateFlow<List<Station>>
        stationsFlow.value = listOf(stationA, stationB)
        
        val userLocation = Location("").apply {
            latitude = 51.51
            longitude = -0.11
        }
        
        val result = viewModel.findNearestStation(userLocation)
        assertEquals(stationA, result)
    }
}
