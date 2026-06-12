package com.timememorial.app.ui.home

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.timememorial.app.R
import com.timememorial.app.data.model.Memory

class MemoryAdapter(
    private val items: List<Memory>,
    private val onClick: (Memory) -> Unit = {}
) : RecyclerView.Adapter<MemoryAdapter.ViewHolder>() {

    private val tagColors = mapOf(
        "love" to R.color.miui_tag_love,
        "birthday" to R.color.miui_tag_birthday,
        "travel" to R.color.miui_tag_travel,
        "work" to R.color.miui_tag_work,
        "favorite" to R.color.miui_tag_favorite,
        "other" to R.color.miui_tag_other
    )

    private val coverGradients = mapOf(
        "love" to R.drawable.bg_memory_cover_love,
        "birthday" to R.drawable.bg_memory_cover_birthday,
        "travel" to R.drawable.bg_memory_cover_travel,
        "work" to R.drawable.bg_memory_cover_work
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverBg: View = view.findViewById(R.id.coverBg)
        val tvTag: TextView = view.findViewById(R.id.tvMemoryTag)
        val tvTitle: TextView = view.findViewById(R.id.tvMemoryTitle)
        val tvDate: TextView = view.findViewById(R.id.tvMemoryDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        // Cover gradient
        val gradientRes = coverGradients[item.tag] ?: R.drawable.bg_memory_cover_work
        holder.coverBg.setBackgroundResource(gradientRes)

        // Tag
        holder.tvTag.text = when(item.tag) {
            "love" -> "爱情"
            "birthday" -> "生日"
            "travel" -> "旅行"
            "work" -> "工作"
            "favorite" -> "收藏"
            else -> "其他"
        }
        val tagBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 36f
            setColor(ContextCompat.getColor(ctx, tagColors[item.tag] ?: R.color.miui_tag_other))
        }
        holder.tvTag.background = tagBg

        holder.tvTitle.text = item.title
        holder.tvDate.text = item.date
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}