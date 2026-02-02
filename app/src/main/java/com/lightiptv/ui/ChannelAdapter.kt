package com.lightiptv.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.lightiptv.R
import com.lightiptv.models.Channel

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = getItem(position)
        holder.bind(channel)
        
        holder.itemView.setOnClickListener {
            onChannelClick(channel)
        }
        
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.1f else 1f)
                .scaleY(if (hasFocus) 1.1f else 1f)
                .setDuration(150)
                .start()
            
            v.setBackgroundColor(
                if (hasFocus) Color.parseColor("#44FFFFFF") else Color.TRANSPARENT
            )
        }
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivLogo: ImageView = view.findViewById(R.id.ivChannelLogo)
        private val tvName: TextView = view.findViewById(R.id.tvChannelName)
        
        fun bind(channel: Channel) {
            tvName.text = channel.name
            
            if (channel.logo.isNotEmpty()) {
                ivLogo.load(channel.logo) {
                    crossfade(false)
                    placeholder(R.drawable.ic_channel_placeholder)
                    error(R.drawable.ic_channel_placeholder)
                    transformations(RoundedCornersTransformation(8f))
                }
            } else {
                ivLogo.setImageResource(R.drawable.ic_channel_placeholder)
            }
            
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}
