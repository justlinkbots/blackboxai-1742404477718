package com.example.filetransfer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.filetransfer.R
import com.example.filetransfer.data.Server
import com.google.android.material.card.MaterialCardView

class ServerAdapter(private val onServerSelected: (Server) -> Unit) :
    ListAdapter<Server, ServerAdapter.ServerViewHolder>(ServerDiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = getItem(position)
        holder.bind(server, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            
            onServerSelected(server)
        }
    }

    class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val nameTextView: TextView = itemView.findViewById(R.id.serverName)
        private val addressTextView: TextView = itemView.findViewById(R.id.serverAddress)
        private val statusIndicator: ImageView = itemView.findViewById(R.id.statusIndicator)

        fun bind(server: Server, isSelected: Boolean) {
            nameTextView.text = server.name
            addressTextView.text = server.fullAddress
            cardView.isChecked = isSelected

            val statusColor = when {
                server.isAvailable -> R.color.green_500
                else -> R.color.red_500
            }
            
            statusIndicator.setColorFilter(
                ContextCompat.getColor(itemView.context, statusColor),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    private class ServerDiffCallback : DiffUtil.ItemCallback<Server>() {
        override fun areItemsTheSame(oldItem: Server, newItem: Server): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Server, newItem: Server): Boolean {
            return oldItem == newItem &&
                   oldItem.isSelected == newItem.isSelected &&
                   oldItem.isAvailable == newItem.isAvailable
        }
    }
}