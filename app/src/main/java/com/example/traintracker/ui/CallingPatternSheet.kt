package com.example.traintracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traintracker.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CallingPatternSheet : BottomSheetDialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var stopsAdapter: StopsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_calling_pattern, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title: TextView = view.findViewById(R.id.sheetTitle)
        val recyclerView: RecyclerView = view.findViewById(R.id.stopsRecyclerView)
        val progressBar: ProgressBar = view.findViewById(R.id.sheetProgressBar)
        val errorText: TextView = view.findViewById(R.id.sheetErrorText)

        stopsAdapter = StopsAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = stopsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.callingPatternUiState.collect { state ->
                    progressBar.isVisible = state is CallingPatternUiState.Loading
                    errorText.isVisible = state is CallingPatternUiState.Error
                    recyclerView.isVisible = state is CallingPatternUiState.Success

                    when(state) {
                        is CallingPatternUiState.Success -> {
                            val isArrivals = viewModel.isArrivals.value
                            val stationName = if (isArrivals) {
                                state.stops.firstOrNull()?.stationName ?: ""
                            } else {
                                state.stops.lastOrNull()?.stationName ?: ""
                            }
                            
                            val stringRes = if (isArrivals) R.string.service_from else R.string.service_to
                            title.text = getString(stringRes, stationName)
                            stopsAdapter.updateStops(state.stops)
                        }
                        is CallingPatternUiState.Error -> {
                            errorText.text = state.message
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearServiceTimetable()
    }

    companion object {
        const val TAG = "CallingPatternSheet"
    }
}
