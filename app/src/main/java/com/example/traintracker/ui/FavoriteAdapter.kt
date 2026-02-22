package com.example.traintracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.traintracker.R
import com.example.traintracker.data.model.FavoriteStation

class FavoriteAdapter(
    private val onFavoriteClick: (FavoriteStation) -> Unit,
    private val onFavoriteDelete: (FavoriteStation) -> Unit
) : ListAdapter<FavoriteStation, FavoriteAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    class FavoriteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.stationName)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_station, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val station = getItem(position)
        holder.nameText.text = station.stationName
        holder.itemView.setOnClickListener { onFavoriteClick(station) }
        holder.deleteButton.setOnClickListener { onFavoriteDelete(station) }
    }

    private class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteStation>() {
        override fun areItemsTheSame(oldItem: FavoriteStation, newItem: FavoriteStation): Boolean {
            return oldItem.crsCode == newItem.crsCode
        }
        override fun areContentsTheSame(oldItem: FavoriteStation, newItem: FavoriteStation): Boolean {
            return oldItem == newItem
        }
    }
}
