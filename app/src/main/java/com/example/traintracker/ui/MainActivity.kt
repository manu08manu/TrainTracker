package com.example.traintracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.traintracker.R
import com.example.traintracker.data.Constants
import com.example.traintracker.data.model.Station
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val adapter by lazy { 
        ServiceAdapter(false) { service ->
            service.service?.let { serviceId ->
                viewModel.fetchServiceTimetable(serviceId)
                CallingPatternSheet().show(supportFragmentManager, CallingPatternSheet.TAG)
            }
        }
    }
    private val favoriteAdapter by lazy { 
        FavoriteAdapter(
            onFavoriteClick = { fav ->
                autoCompleteTextView.setText(getString(R.string.station_format, fav.stationName, fav.crsCode))
                viewModel.fetchStationData(fav.crsCode)
            },
            onFavoriteDelete = { fav ->
                viewModel.removeFavorite(fav)
            }
        )
    }
    
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var submitButton: Button
    private lateinit var favoriteButton: ImageButton
    private lateinit var locationButton: ImageButton
    private lateinit var themeButton: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var lastUpdatedText: TextView
    private lateinit var emptyState: TextView
    private lateinit var noConnectionBar: TextView
    private lateinit var favoritesRecyclerView: RecyclerView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { noConnectionBar.visibility = View.GONE }
        }

        override fun onLost(network: Network) {
            runOnUiThread { noConnectionBar.visibility = View.VISIBLE }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        connectivityManager = getSystemService<ConnectivityManager>()!!

        initViews()
        setupRecyclerViews()
        setupListeners()
        setupObservers()
        monitorConnection()
    }

    private fun initViews() {
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.progressBar)
        favoriteButton = findViewById(R.id.favoriteButton)
        locationButton = findViewById(R.id.locationButton)
        themeButton = findViewById(R.id.themeButton)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        lastUpdatedText = findViewById(R.id.lastUpdatedText)
        emptyState = findViewById(R.id.emptyState)
        noConnectionBar = findViewById(R.id.noConnectionBar)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        
        updateThemeButtonIcon()
    }

    private fun setupRecyclerViews() {
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoriteAdapter
        }
    }

    private fun setupListeners() {
        findViewById<TabLayout>(R.id.tabLayout).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isArrivals = tab?.position == 1
                submitButton.text = if (isArrivals) getString(R.string.get_arrivals) else getString(R.string.get_departures)
                viewModel.setMode(isArrivals)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val station = parent.getItemAtPosition(position) as Station
            viewModel.updateCurrentStation(station.crsCode)
        }

        submitButton.setOnClickListener {
            val input = autoCompleteTextView.text.toString().trim()
            val foundStation = viewModel.stations.value.find { 
                it.crsCode.equals(input, ignoreCase = true) || 
                input.contains("(${it.crsCode})", ignoreCase = true) ||
                it.stationName.equals(input, ignoreCase = true) ||
                (it.stationName.startsWith(Constants.LONDON_PREFIX, ignoreCase = true) && 
                 it.stationName.substring(Constants.LONDON_PREFIX.length).equals(input, ignoreCase = true))
            }

            if (foundStation != null) {
                viewModel.fetchStationData(foundStation.crsCode)
            } else {
                Toast.makeText(this, getString(R.string.select_valid_station), Toast.LENGTH_SHORT).show()
            }
        }

        favoriteButton.setOnClickListener {
            if (!viewModel.toggleFavorite()) {
                Toast.makeText(this, getString(R.string.select_valid_station), Toast.LENGTH_SHORT).show()
            }
        }

        locationButton.setOnClickListener {
            checkLocationPermission()
        }
        
        themeButton.setOnClickListener {
            toggleTheme()
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun isDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun toggleTheme() {
        if (isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun updateThemeButtonIcon() {
        if (isDarkMode()) {
            themeButton.setImageResource(R.drawable.ic_moon)
        } else {
            themeButton.setImageResource(R.drawable.ic_sun)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stations.collectLatest { stations ->
                        if (stations.isNotEmpty()) {
                            setupStationAdapter(stations)
                        }
                    }
                }
                launch {
                    viewModel.uiState.collectLatest { state ->
                        progressBar.isVisible = state is UiState.Loading && !swipeRefreshLayout.isRefreshing
                        emptyState.isVisible = state is UiState.Error || (state is UiState.Success && state.services.isEmpty())
                        
                        if (state !is UiState.Loading) {
                            swipeRefreshLayout.isRefreshing = false
                        }

                        when (state) {
                            is UiState.Success -> {
                                emptyState.text = getString(R.string.no_services_found)
                                adapter.submitList(state.services)
                                adapter.updateMode(viewModel.isArrivals.value)
                            }
                            is UiState.Error -> {
                                emptyState.text = state.message
                                adapter.submitList(emptyList())
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    viewModel.lastUpdated.collectLatest { time ->
                        lastUpdatedText.isVisible = time != null
                        lastUpdatedText.text = time?.let { getString(R.string.last_updated, it) }
                    }
                }
                launch {
                    viewModel.isCurrentStationFavorite.collectLatest { isFav ->
                        favoriteButton.setImageResource(
                            if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
                        )
                        if (isFav) {
                            favoriteButton.imageTintList = null 
                        } else {
                            val typedValue = TypedValue()
                            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                            favoriteButton.imageTintList = ColorStateList.valueOf(typedValue.data)
                        }
                    }
                }
                launch {
                    viewModel.favorites.collectLatest { favorites ->
                        favoritesRecyclerView.isVisible = favorites.isNotEmpty()
                        favoriteAdapter.submitList(favorites)
                    }
                }
            }
        }
    }

    private fun setupStationAdapter(stations: List<Station>) {
        val stationAdapter = object : ArrayAdapter<Station>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            stations
        ) {
            private val originalList = ArrayList(stations)

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val station = getItem(position)
                view.text = station?.let { getString(R.string.station_format, it.stationName, it.crsCode) } ?: ""
                return view
            }

            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        val suggestions = ArrayList<Station>()

                        if (!constraint.isNullOrBlank()) {
                            val pattern = constraint.toString().lowercase().trim()
                            val londonPrefix = Constants.LONDON_PREFIX.lowercase()
                            for (station in originalList) {
                                val nameLower = station.stationName.lowercase()
                                val matchesName = nameLower.startsWith(pattern) || 
                                                 (nameLower.startsWith(londonPrefix) && nameLower.removePrefix(londonPrefix).startsWith(pattern))
                                val matchesCode = station.crsCode.lowercase().startsWith(pattern)

                                if (matchesName || matchesCode) suggestions.add(station)
                            }
                        }

                        results.values = suggestions
                        results.count = suggestions.size
                        return results
                    }

                    @Suppress("UNCHECKED_CAST")
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        clear()
                        if (results != null && results.count > 0) {
                            addAll(results.values as List<Station>)
                        }
                        notifyDataSetChanged()
                    }

                    override fun convertResultToString(resultValue: Any?): CharSequence {
                        val station = resultValue as Station
                        return getString(R.string.station_format, station.stationName, station.crsCode)
                    }
                }
            }
        }
        autoCompleteTextView.setAdapter(stationAdapter)
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val nearest = viewModel.findNearestStation(location)
                if (nearest != null) {
                    autoCompleteTextView.setText(getString(R.string.station_format, nearest.stationName, nearest.crsCode))
                    viewModel.fetchStationData(nearest.crsCode)
                }
            }
        }
    }

    private fun monitorConnection() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        
        // Initial check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        noConnectionBar.isVisible = !isConnected
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
