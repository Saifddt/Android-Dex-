package com.dexlauncher.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class TaskbarAdapter(
    private var apps: MutableList<AppInfo>,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, Int) -> Unit
) : RecyclerView.Adapter<TaskbarAdapter.TaskbarViewHolder>() {

    class TaskbarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.taskbarAppIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskbarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_taskbar_app, parent, false)
        return TaskbarViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskbarViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener {
            onLongClick(app, holder.adapterPosition)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateList(newApps: MutableList<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        if (position in apps.indices) {
            apps.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
