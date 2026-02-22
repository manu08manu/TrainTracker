package com.example.traintracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.traintracker.R
import com.example.traintracker.data.model.Stop

class StopsAdapter(private var stops: List<Stop>) : RecyclerView.Adapter<StopsAdapter.StopViewHolder>() {

    fun updateStops(newStops: List<Stop>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = stops.size
            override fun getNewListSize(): Int = newStops.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return stops[oldItemPosition].stationName == newStops[newItemPosition].stationName
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return stops[oldItemPosition] == newStops[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        stops = newStops
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stop, parent, false)
        return StopViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(stops[position], position == stops.size - 1)
    }

    override fun getItemCount() = stops.size

    class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stationName: TextView = itemView.findViewById(R.id.stopStationName)
        private val timeSch: TextView = itemView.findViewById(R.id.stopTimeSch)
        private val timeEst: TextView = itemView.findViewById(R.id.stopTimeEst)
        private val timelineLine: View = itemView.findViewById(R.id.timelineVerticalLine)

        fun bind(stop: Stop, isLast: Boolean) {
            stationName.text = stop.stationName
            
            val schTime = listOfNotNull(stop.aimedArrivalTime, stop.aimedDepartureTime).firstOrNull { it.isNotBlank() } ?: "--:--"
            val estTimeRaw = listOfNotNull(stop.expectedArrivalTime, stop.expectedDepartureTime).firstOrNull { it.isNotBlank() }
            val estTime = estTimeRaw ?: schTime
            
            timeSch.text = itemView.context.getString(R.string.stop_sch_time, schTime)
            timeEst.text = itemView.context.getString(R.string.stop_est_time, estTime)
            
            timelineLine.visibility = if (isLast) View.GONE else View.VISIBLE
        }
    }
}
