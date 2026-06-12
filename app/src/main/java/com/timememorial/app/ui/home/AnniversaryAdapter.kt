package com.timememorial.app.ui.home

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.timememorial.app.R
import com.timememorial.app.data.model.Anniversary

class AnniversaryAdapter(
    private val items: List<Anniversary>
) : RecyclerView.Adapter<AnniversaryAdapter.ViewHolder>() {

    private val tagColors = mapOf(
        "love" to R.color.miui_tag_love,
        "birthday" to R.color.miui_tag_birthday,
        "travel" to R.color.miui_tag_travel,
        "work" to R.color.miui_tag_work,
        "favorite" to R.color.miui_tag_favorite,
        "other" to R.color.miui_tag_other
    )

    private val tagNames = mapOf(
        "love" to "爱情",
        "birthday" to "生日",
        "travel" to "旅行",
        "work" to "工作",
        "favorite" to "收藏",
        "other" to "其他"
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tvAnniTag)
        val tvTitle: TextView = view.findViewById(R.id.tvAnniTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvAnniDesc)
        val tvCountdown: TextView = view.findViewById(R.id.tvCountdown)
        val tvUnit: TextView = view.findViewById(R.id.tvCountdownUnit)
        val tvDate: TextView = view.findViewById(R.id.tvAnniDate)
        val progress: ProgressBar = view.findViewById(R.id.progressAnni)
        val tvProgress: TextView = view.findViewById(R.id.tvAnniProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_anniversary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        // Tag pill
        holder.tvTag.text = tagNames[item.tag] ?: item.tag
        val tagBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 36f
            setColor(ContextCompat.getColor(ctx, tagColors[item.tag] ?: R.color.miui_tag_other))
        }
        holder.tvTag.background = tagBg

        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.desc
        holder.tvDate.text = item.date

        // Countdown
        if (item.isExpired) {
            holder.tvCountdown.text = "0"
            holder.tvCountdown.setTextColor(ContextCompat.getColor(ctx, R.color.miui_green_start))
            holder.tvUnit.text = ctx.getString(R.string.expired)
        } else {
            holder.tvCountdown.text = item.daysRemaining.toString()
            holder.tvCountdown.setTextColor(ContextCompat.getColor(ctx, R.color.miui_brand))
            holder.tvUnit.text = ctx.getString(R.string.days_unit)
        }

        // Progress
        val progressPct = if (item.isExpired || item.totalDays <= 0) 100
        else ((item.totalDays - item.daysRemaining) * 100 / item.totalDays).coerceIn(0, 100)
        holder.progress.progress = progressPct
        holder.tvProgress.text = "$progressPct%"
    }

    override fun getItemCount() = items.size
}