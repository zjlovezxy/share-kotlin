package io.github.zjlovezxy.sharekt.core.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.ui.databinding.ItemCommentBinding

class CommentAdapter : ListAdapter<Comment, CommentAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    open class ViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            with(binding) {
                avatar.loadUrl(comment.user.avatarUrl)
                name.text = comment.user.screenName
                content.setRichText(comment.displayText)
                val reply = comment.replyTo?.user?.screenName
                meta.text = buildString {
                    append(formatRelativeTime(comment.createdAt))
                    if (!reply.isNullOrEmpty()) append("  ·  回复 @").append(reply)
                }
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
        }
    }
}
