package com.example.traintracker.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.traintracker.R
import com.example.traintracker.data.model.TrainService
import java.text.SimpleDateFormat
import java.util.Locale

class ServiceAdapter(
    private var isArrivals: Boolean,
    private val onItemClick: (TrainService) -> Unit
) : ListAdapter<TrainService, ServiceAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    fun updateMode(newIsArrivals: Boolean) {
        if (isArrivals != newIsArrivals) {
            isArrivals = newIsArrivals
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.service_card, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position), isArrivals, onItemClick)
    }

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val destination: TextView = itemView.findViewById(R.id.serviceDestination)
        private val times: TextView = itemView.findViewById(R.id.serviceTimes)
        private val platform: TextView = itemView.findViewById(R.id.servicePlatform)
        private val status: TextView = itemView.findViewById(R.id.serviceStatus)
        private val operatorName: TextView = itemView.findViewById(R.id.operatorName)
        private val operatorLogo: ImageView = itemView.findViewById(R.id.operatorLogo)

        @SuppressLint("DiscouragedApi")
        fun bind(service: TrainService, isArrivals: Boolean, onItemClick: (TrainService) -> Unit) {
            val context = itemView.context
            destination.text = if (isArrivals) service.originName else service.destinationName
            operatorName.text = service.operatorName

            val schTime = if (isArrivals) service.aimedArrivalTime else service.aimedDepartureTime
            val expTime = if (isArrivals) service.expectedArrivalTime else service.expectedDepartureTime
            
            times.text = context.getString(R.string.service_times, schTime ?: "--:--", expTime ?: "--:--")
            platform.text = context.getString(R.string.service_platform, service.platform ?: context.getString(R.string.no_platform))
            
            val delayMins = calculateDelay(schTime, expTime)
            if (delayMins > 0) {
                status.text = context.getString(R.string.delayed_mins, delayMins)
                status.setTextColor(getDelayColor(delayMins))
            } else {
                status.text = service.status
                val statusText = service.status?.lowercase() ?: ""
                when {
                    statusText.contains("on time") -> status.setTextColor("#4CAF50".toColorInt())
                    statusText.contains("cancelled") -> status.setTextColor(Color.RED)
                    statusText.contains("starts here") -> status.setTextColor("#4CAF50".toColorInt())
                    else -> status.setTextColor(Color.GRAY)
                }
            }

            // Load local operator logo
            val opCode = service.operator?.lowercase()?.trim()
            val resId = if (!opCode.isNullOrEmpty()) {
                val resourceName = "logo_$opCode"
                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            } else 0

            if (resId != 0) {
                operatorLogo.setImageResource(resId)
            } else {
                operatorLogo.setImageResource(R.drawable.ic_launcher_foreground)
            }

            itemView.setOnClickListener { onItemClick(service) }
        }

        private fun calculateDelay(sch: String?, exp: String?): Int {
            if (sch == null || exp == null || exp == "On time" || exp == "Cancelled" || sch == exp) return 0
            if (!exp.contains(":")) return 0
            
            try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val schDate = sdf.parse(sch)
                val expDate = sdf.parse(exp)
                if (schDate != null && expDate != null) {
                    var diff = expDate.time - schDate.time
                    if (diff < -12 * 60 * 60 * 1000) diff += 24 * 60 * 60 * 1000
                    if (diff > 12 * 60 * 60 * 1000) diff -= 24 * 60 * 60 * 1000
                    
                    val mins = (diff / (1000 * 60)).toInt()
                    return if (mins > 0) mins else 0
                }
            } catch (_: Exception) {}
            return 0
        }

        private fun getDelayColor(mins: Int): Int {
            return when {
                mins >= 120 -> "#7B1FA2".toColorInt()
                mins >= 60 -> "#B71C1C".toColorInt()
                mins >= 30 -> Color.RED
                mins >= 15 -> "#F57C00".toColorInt()
                else -> "#FBC02D".toColorInt()
            }
        }
    }

    private class ServiceDiffCallback : DiffUtil.ItemCallback<TrainService>() {
        override fun areItemsTheSame(oldItem: TrainService, newItem: TrainService): Boolean {
            return oldItem.service == newItem.service
        }
        override fun areContentsTheSame(oldItem: TrainService, newItem: TrainService): Boolean {
            return oldItem == newItem
        }
    }
}
