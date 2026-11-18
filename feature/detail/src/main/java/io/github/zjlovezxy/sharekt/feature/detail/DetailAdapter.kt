package io.github.zjlovezxy.sharekt.feature.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.ui.CommentAdapter
import io.github.zjlovezxy.sharekt.core.ui.StatusBinder
import io.github.zjlovezxy.sharekt.core.ui.databinding.ItemCommentBinding
import io.github.zjlovezxy.sharekt.core.ui.databinding.ItemStatusBinding

/**
 * 详情页 = 1 个微博头 + N 条评论，用一个 RecyclerView 承载，
 * 避免 RecyclerView 套 ScrollView 的测量灾难。
 */
class DetailAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is Status) TYPE_STATUS else TYPE_COMMENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_STATUS) {
            StatusHolder(ItemStatusBinding.inflate(inflater, parent, false))
        } else {
            CommentHolder(ItemCommentBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Status -> (holder as StatusHolder).bind(item)
            is Comment -> (holder as CommentHolder).bind(item)
        }
    }

    private class StatusHolder(private val binding: ItemStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(status: Status) = StatusBinder.bind(binding, status) { /* 已在详情页，不跳转 */ }
    }

    private class CommentHolder(binding: ItemCommentBinding) :
        CommentAdapter.ViewHolder(binding)

    private companion object {
        const val TYPE_STATUS = 0
        const val TYPE_COMMENT = 1

        val DIFF = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
                oldItem is Status && newItem is Status -> oldItem.id == newItem.id
                oldItem is Comment && newItem is Comment -> oldItem.id == newItem.id
                else -> false
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = oldItem == newItem
        }
    }
}
