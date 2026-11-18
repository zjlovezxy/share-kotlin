package io.github.zjlovezxy.sharekt.core.ui

import android.view.View
import android.widget.ImageView
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.ui.databinding.ItemStatusBinding

/** 微博卡片的绑定逻辑，时间线/详情/搜索/个人页共用一份。 */
object StatusBinder {

    fun bind(binding: ItemStatusBinding, status: Status, onClick: (Status) -> Unit) {
        with(binding) {
            avatar.loadUrl(status.user.avatarUrl)
            name.text = status.user.screenName.ifEmpty { "微博用户" }

            val source = status.source.stripHtml()
            meta.text = listOf(formatRelativeTime(status.createdAt), source)
                .filter { it.isNotEmpty() }
                .joinToString("  ·  ")

            content.setRichText(status.displayText)

            val retweet = status.retweeted
            if (retweet == null) {
                retweetBox.visibility = View.GONE
            } else {
                retweetBox.visibility = View.VISIBLE
                retweetName.text = "@${retweet.user.screenName}"
                retweetContent.setRichText(retweet.displayText)
                bindImages(retweetImages, retweet.imageUrls)
            }

            bindImages(images, status.imageUrls)

            reposts.text = "转发 ${status.repostsCount}"
            comments.text = "评论 ${status.commentsCount}"
            likes.text = "赞 ${status.likesCount}"

            root.setOnClickListener { onClick(status) }
        }
    }

    private fun bindImages(container: NineGridLayout, urls: List<String>) {
        container.removeAllViews()
        container.gap = (3 * container.resources.displayMetrics.density).toInt()
        if (urls.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        urls.take(MAX_IMAGES).forEach { url ->
            val image = ImageView(container.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                loadUrl(url)
            }
            container.addView(image)
        }
    }

    private const val MAX_IMAGES = 9
}
