package io.github.zjlovezxy.sharekt.core.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.ui.databinding.ItemStatusBinding

/** 时间线/搜索/个人页共用的微博卡片。点击整卡进详情。 */
class StatusAdapter(
    private val onClick: (Status) -> Unit,
) : ListAdapter<Status, StatusAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(private val binding: ItemStatusBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(status: Status, onClick: (Status) -> Unit) =
            StatusBinder.bind(binding, status, onClick)
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Status, newItem: Status) = oldItem == newItem
        }
    }
}
