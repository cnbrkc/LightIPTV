package com.lightiptv.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lightiptv.R
import com.lightiptv.models.Category

class CategoryAdapter(
    private val onCategoryClick: (Category, Int) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback()) {
    
    private var selectedPosition = 0
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onCategoryClick(category, position)
        }
        
        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            holder.itemView.setBackgroundColor(
                if (hasFocus) Color.parseColor("#44FFFFFF") else Color.TRANSPARENT
            )
        }
    }
    
    fun setSelected(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        private val tvCount: TextView = view.findViewById(R.id.tvChannelCount)
        
        fun bind(category: Category, isSelected: Boolean) {
            tvName.text = category.name
            tvCount.text = "${category.channels.size} kanal"
            
            itemView.setBackgroundColor(
                if (isSelected) Color.parseColor("#33FFFFFF") else Color.TRANSPARENT
            )
            
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
    }
}
